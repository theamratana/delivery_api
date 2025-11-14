package com.delivery.deliveryapi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.deliveryapi.controller.DeliveryController.CreateDeliveryRequest;
import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.DeliveryItem;
import com.delivery.deliveryapi.model.DeliveryPhoto;
import com.delivery.deliveryapi.model.Product;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserType;
import com.delivery.deliveryapi.repo.CompanyRepository;
import com.delivery.deliveryapi.repo.DeliveryItemRepository;
import com.delivery.deliveryapi.repo.DeliveryPhotoRepository;
import com.delivery.deliveryapi.repo.ProductRepository;
import com.delivery.deliveryapi.repo.UserRepository;

@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);
    private static final String DELIVERY_TYPE_COMPANY = "COMPANY";
    private static final String DELIVERY_TYPE_DRIVER = "DRIVER";

    private final DeliveryItemRepository deliveryItemRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final DeliveryPhotoRepository deliveryPhotoRepository;
    private final DeliveryPricingService deliveryPricingService;
    private final ProductService productService;
    private final ProductRepository productRepository;

    public DeliveryService(DeliveryItemRepository deliveryItemRepository,
                          UserRepository userRepository,
                          CompanyRepository companyRepository,
                          DeliveryPhotoRepository deliveryPhotoRepository,
                          DeliveryPricingService deliveryPricingService,
                          ProductService productService,
                          ProductRepository productRepository) {
        this.deliveryItemRepository = deliveryItemRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.deliveryPhotoRepository = deliveryPhotoRepository;
        this.deliveryPricingService = deliveryPricingService;
        this.productService = productService;
        this.productRepository = productRepository;
    }

    @Transactional
    public DeliveryItem createDelivery(User sender, CreateDeliveryRequest request) {
        log.info("Creating batch delivery for sender: {} to receiver phone: {} with {} items", 
            sender.getId(), request.getReceiverPhone(), 
            request.getItems() != null ? request.getItems().size() : 0);

        // Validate request
        validateCreateDeliveryRequest(request);

        // Find or create receiver
        User receiver = findOrCreateReceiver(request.getReceiverPhone(), request.getReceiverName());
        boolean autoCreatedReceiver = receiver.isIncomplete();
        log.info("Using receiver: {} ({})", receiver.getId(), receiver.getPhoneE164());

        // Find or create delivery company/driver
        Company deliveryCompany = null;
        User deliveryDriver = null;
        boolean autoCreatedCompany = false;
        boolean autoCreatedDriver = false;

        if (DELIVERY_TYPE_COMPANY.equalsIgnoreCase(request.getDeliveryType())) {
            deliveryCompany = findOrCreateCompany(request.getCompanyName());
            autoCreatedCompany = true;
            log.info("Using delivery company: {} ({})", deliveryCompany.getId(), deliveryCompany.getName());
        } else if (DELIVERY_TYPE_DRIVER.equalsIgnoreCase(request.getDeliveryType())) {
            deliveryDriver = findOrCreateDriver(request.getDriverPhone());
            autoCreatedDriver = deliveryDriver.isIncomplete();
            log.info("Using delivery driver: {} ({})", deliveryDriver.getId(), deliveryDriver.getPhoneE164());
        }

        // Auto-populate pickup information from sender's company if not provided
        String pickupAddress = request.getPickupAddress();
        String pickupProvince = request.getPickupProvince();
        String pickupDistrict = request.getPickupDistrict();

        if ((pickupAddress == null || pickupAddress.trim().isEmpty()) &&
            sender.getCompany() != null && sender.getCompany().getAddress() != null && !sender.getCompany().getAddress().trim().isEmpty()) {
            pickupAddress = sender.getCompany().getAddress();
        }
        if ((pickupProvince == null || pickupProvince.trim().isEmpty()) &&
            sender.getCompany() != null && sender.getCompany().getDistrict() != null &&
            sender.getCompany().getDistrict().getProvince() != null) {
            pickupProvince = sender.getCompany().getDistrict().getProvince().getName();
        }
        if ((pickupDistrict == null || pickupDistrict.trim().isEmpty()) &&
            sender.getCompany() != null && sender.getCompany().getDistrict() != null) {
            pickupDistrict = sender.getCompany().getDistrict().getName();
        }

        // Calculate or use provided delivery fee (ONE FEE for entire delivery, all items)
        BigDecimal deliveryFee = request.getDeliveryFee();
        if (deliveryFee == null || deliveryFee.signum() <= 0) {
            // Only calculate if fee not provided or invalid
            deliveryFee = deliveryPricingService.calculateDeliveryFee(sender, request);
            log.debug("Fee not provided, calculated: {}", deliveryFee);
        } else {
            log.debug("Using frontend-provided fee: {}", deliveryFee);
        }

        // Process each item in the batch and return the first one created
        // (primary delivery item for response, all items share same receiver/company/driver/fee)
        DeliveryItem firstDelivery = null;

        for (int itemIndex = 0; itemIndex < request.getItems().size(); itemIndex++) {
            var itemPayload = request.getItems().get(itemIndex);
            log.info("Processing item {}/{}: productName={}, description={}", 
                itemIndex + 1, request.getItems().size(), itemPayload.getProductName(), itemPayload.getItemDescription());

            // Handle product selection/creation for this item
            Product product = null;
            boolean autoCreatedProduct = false;

            if (itemPayload.getProductId() != null) {
                // Use existing product by ID
                Optional<Product> existingProduct = productRepository.findById(itemPayload.getProductId());
                if (existingProduct.isPresent()) {
                    product = existingProduct.get();
                    if (!product.getCompany().getId().equals(sender.getCompany().getId())) {
                        throw new IllegalArgumentException("Access denied: Product belongs to different company");
                    }
                    product.setUsageCount(product.getUsageCount() + 1);
                    product.setLastUsedAt(OffsetDateTime.now());
                    productRepository.save(product);
                    log.info("Using existing product by ID: {}", product.getId());
                } else {
                    throw new IllegalArgumentException("Product not found: " + itemPayload.getProductId());
                }
            } else if (itemPayload.getProductName() != null && !itemPayload.getProductName().trim().isEmpty()) {
                // PRIORITY 1: Search by product name (required field)
                String searchName = itemPayload.getProductName().trim();
                List<Product> existingProducts = productRepository.searchProductsByName(sender.getCompany().getId(), searchName);
                
                if (!existingProducts.isEmpty()) {
                    // Found existing product with this name - reuse it
                    product = existingProducts.get(0);
                    product.setUsageCount(product.getUsageCount() + 1);
                    product.setLastUsedAt(OffsetDateTime.now());
                    productRepository.save(product);
                    log.info("Reusing existing product by name '{}': {}", searchName, product.getId());
                } else {
                    // No existing product found - auto-create with product name
                    product = productService.createProductFromDelivery(sender, 
                        itemPayload.getProductName(), // Use productName as the product name
                        itemPayload.getEstimatedValue(), deliveryFee);
                    productRepository.flush(); // Ensure product is persisted immediately for next item search
                    autoCreatedProduct = true;
                    log.info("Created new product: {} (ID: {})", itemPayload.getProductName(), product.getId());
                }
            } else if (itemPayload.getItemDescription() != null && !itemPayload.getItemDescription().trim().isEmpty()) {
                // FALLBACK: If productName is missing, use itemDescription for first creation
                String fallbackName = itemPayload.getItemDescription().length() > 100 ? 
                    itemPayload.getItemDescription().substring(0, 100) : itemPayload.getItemDescription();
                
                List<Product> existingProducts = productRepository.searchProductsByName(sender.getCompany().getId(), fallbackName);
                if (!existingProducts.isEmpty()) {
                    product = existingProducts.get(0);
                    product.setUsageCount(product.getUsageCount() + 1);
                    product.setLastUsedAt(OffsetDateTime.now());
                    productRepository.save(product);
                    log.info("Reusing existing product by description fallback: {}", product.getId());
                } else {
                    // Auto-create using description as fallback
                    product = productService.createProductFromDelivery(sender, fallbackName, 
                        itemPayload.getEstimatedValue(), deliveryFee);
                    productRepository.flush(); // Ensure product is persisted immediately
                    autoCreatedProduct = true;
                    log.info("Created new product from description fallback: {} (ID: {})", fallbackName, product.getId());
                }
            } else {
                throw new IllegalArgumentException("Item " + (itemIndex + 1) + ": productName or itemDescription is required");
            }

            // Create delivery item for this product
            DeliveryItem delivery = new DeliveryItem();
            delivery.setSender(sender);
            delivery.setReceiver(receiver);
            delivery.setDeliveryCompany(deliveryCompany);
            delivery.setDeliveryDriver(deliveryDriver);
            delivery.setItemDescription(itemPayload.getItemDescription());
            delivery.setPaymentMethod(com.delivery.deliveryapi.model.PaymentMethod.fromCode(
                itemPayload.getPaymentMethod() != null ? itemPayload.getPaymentMethod() : request.getPaymentMethod()));

            delivery.setPickupAddress(pickupAddress);
            delivery.setPickupProvince(pickupProvince);
            delivery.setPickupDistrict(pickupDistrict);

            delivery.setDeliveryAddress(request.getDeliveryAddress());
            delivery.setDeliveryProvince(request.getDeliveryProvince());
            delivery.setDeliveryDistrict(request.getDeliveryDistrict());
            delivery.setItemValue(itemPayload.getEstimatedValue());
            delivery.setQuantity(itemPayload.getQuantity()); // Set product quantity from payload
            
            // All items share the SAME delivery fee (calculated once at batch level)
            delivery.setDeliveryFee(deliveryFee);
            delivery.setEstimatedDeliveryTime(OffsetDateTime.now().plusHours(2));

            delivery.setProduct(product);
            delivery.setAutoCreatedCompany(autoCreatedCompany);
            delivery.setAutoCreatedDriver(autoCreatedDriver);
            delivery.setAutoCreatedReceiver(autoCreatedReceiver && itemIndex == 0); // Only first item marks auto-created receiver
            delivery.setAutoCreatedProduct(autoCreatedProduct);
            delivery.setFeeAutoCalculated(true);

            delivery = deliveryItemRepository.save(delivery);
            log.info("Created delivery item {}/{}: {}", itemIndex + 1, request.getItems().size(), delivery.getId());

            // Store first delivery for response
            if (firstDelivery == null) {
                firstDelivery = delivery;
            }

            // Create delivery photos for this item if provided
            if (itemPayload.getItemPhotos() != null && !itemPayload.getItemPhotos().isEmpty()) {
                for (int i = 0; i < itemPayload.getItemPhotos().size(); i++) {
                    DeliveryPhoto photo = new DeliveryPhoto(delivery, itemPayload.getItemPhotos().get(i), i);
                    deliveryPhotoRepository.save(photo);
                }
                log.info("Created {} photos for item {}", itemPayload.getItemPhotos().size(), itemIndex + 1);
            }
        }

        log.info("Batch delivery creation completed with {} items", request.getItems().size());
        return firstDelivery;
    }

    private void validateCreateDeliveryRequest(CreateDeliveryRequest request) {
        validateRequiredFields(request);
        validateDeliveryType(request);
    }

    private void validateRequiredFields(CreateDeliveryRequest request) {
        if (request.getReceiverPhone() == null || request.getReceiverPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Receiver phone is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }
        for (int i = 0; i < request.getItems().size(); i++) {
            var item = request.getItems().get(i);
            if (item.getItemDescription() == null || item.getItemDescription().trim().isEmpty()) {
                throw new IllegalArgumentException("Item " + (i + 1) + ": itemDescription is required");
            }
        }
        // Pickup fields are now optional - will be auto-populated from sender if not provided
        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery address is required");
        }
        if (request.getDeliveryProvince() == null || request.getDeliveryProvince().trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery province is required");
        }
        if (request.getDeliveryDistrict() == null || request.getDeliveryDistrict().trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery district is required");
        }
    }

    private void validateDeliveryType(CreateDeliveryRequest request) {
        String deliveryType = request.getDeliveryType();
        if (!DELIVERY_TYPE_COMPANY.equalsIgnoreCase(deliveryType) &&
            !DELIVERY_TYPE_DRIVER.equalsIgnoreCase(deliveryType)) {
            throw new IllegalArgumentException("Delivery type must be COMPANY or DRIVER");
        }

        if (DELIVERY_TYPE_COMPANY.equalsIgnoreCase(deliveryType)) {
            if (request.getCompanyPhone() == null || request.getCompanyPhone().trim().isEmpty()) {
                throw new IllegalArgumentException("Company phone is required for company deliveries");
            }
        } else if (DELIVERY_TYPE_DRIVER.equalsIgnoreCase(deliveryType) &&
                   (request.getDriverPhone() == null || request.getDriverPhone().trim().isEmpty())) {
            throw new IllegalArgumentException("Driver phone is required for driver deliveries");
        }
    }

    private User findOrCreateReceiver(String phone, String name) {
        String normalizedPhone = normalizePhone(phone);

        Optional<User> existingUser = userRepository.findByPhoneE164(normalizedPhone);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // Create unverified receiver
        User receiver = new User();
        receiver.setPhoneE164(normalizedPhone);
        receiver.setUserType(UserType.CUSTOMER);
        receiver.setIncomplete(true);
        receiver.setActive(true);

        if (name != null && !name.trim().isEmpty()) {
            receiver.setDisplayName(name.trim());
        }

        return userRepository.save(receiver);
    }

    private Company findOrCreateCompany(String name) {
        // Try to find existing company by phone (this is a simplified approach)
        // In a real implementation, you'd need a way to link companies to phone numbers
        Optional<Company> existingCompany = companyRepository.findByName(name);
        if (existingCompany.isPresent()) {
            return existingCompany.get();
        }

        // Create unverified company
        Company company = new Company();
        company.setName(name);
        company.setActive(true);

        return companyRepository.save(company);
    }

    private User findOrCreateDriver(String phone) {
        String normalizedPhone = normalizePhone(phone);

        Optional<User> existingUser = userRepository.findByPhoneE164(normalizedPhone);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getUserType() != UserType.DRIVER) {
                user.setUserType(UserType.DRIVER);
                userRepository.save(user);
            }
            return user;
        }

        // Create unverified driver
        User driver = new User();
        driver.setPhoneE164(normalizedPhone);
        driver.setUserType(UserType.DRIVER);
        driver.setIncomplete(true);
        driver.setActive(true);

        return userRepository.save(driver);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("\\s+", "").trim();
    }
}