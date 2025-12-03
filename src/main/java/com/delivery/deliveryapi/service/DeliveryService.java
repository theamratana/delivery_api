package com.delivery.deliveryapi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.deliveryapi.controller.DeliveryController.CreateDeliveryRequest;
import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.DeliveryItem;
import com.delivery.deliveryapi.model.DeliveryPackage;
import com.delivery.deliveryapi.model.DeliveryPackageStatus;
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
    private final com.delivery.deliveryapi.repo.DeliveryPackageRepository deliveryPackageRepository;

    public DeliveryService(DeliveryItemRepository deliveryItemRepository,
                          UserRepository userRepository,
                          CompanyRepository companyRepository,
                          DeliveryPhotoRepository deliveryPhotoRepository,
                          DeliveryPricingService deliveryPricingService,
                          ProductService productService,
                          ProductRepository productRepository,
                          com.delivery.deliveryapi.repo.DeliveryPackageRepository deliveryPackageRepository) {
        this.deliveryItemRepository = deliveryItemRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.deliveryPhotoRepository = deliveryPhotoRepository;
        this.deliveryPricingService = deliveryPricingService;
        this.productService = productService;
        this.productRepository = productRepository;
        this.deliveryPackageRepository = deliveryPackageRepository;
    }

    @Transactional
    public DeliveryItem createDelivery(User sender, CreateDeliveryRequest request) {
        log.info("Creating batch delivery for sender: {} to receiver phone: {} with {} items", 
            sender.getId(), request.getReceiverPhone(), 
            request.getItems() != null ? request.getItems().size() : 0);

        // Will create DeliveryPackage after fee calculation so fee can be stored on the package

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

        // Create a DeliveryPackage to represent this batch and keep lifecycle metadata
        com.delivery.deliveryapi.model.DeliveryPackage pkg = new com.delivery.deliveryapi.model.DeliveryPackage();
        pkg.setSender(sender);
        pkg.setDeliveryFee(deliveryFee);
        pkg = deliveryPackageRepository.save(pkg);
        UUID batchId = pkg.getId();

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
                        itemPayload.getEstimatedValue(), deliveryFee, itemPayload.getItemPhotos());
                    productRepository.flush(); // Ensure product is persisted immediately for next item search
                    autoCreatedProduct = true;
                    log.info("Created new product: {} (ID: {}) with {} photos", itemPayload.getProductName(), product.getId(), 
                        itemPayload.getItemPhotos() != null ? itemPayload.getItemPhotos().size() : 0);
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
                        itemPayload.getEstimatedValue(), deliveryFee, itemPayload.getItemPhotos());
                    productRepository.flush(); // Ensure product is persisted immediately
                    autoCreatedProduct = true;
                    log.info("Created new product from description fallback: {} (ID: {}) with {} photos", fallbackName, product.getId(),
                        itemPayload.getItemPhotos() != null ? itemPayload.getItemPhotos().size() : 0);
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
            delivery.setItemValue(itemPayload.getPrice());
            delivery.setQuantity(itemPayload.getQuantity()); // Set product quantity from payload
            
            // All items share the SAME delivery fee (calculated once at batch level)
            delivery.setDeliveryFee(deliveryFee);
            
            // Store discount fields
            delivery.setDeliveryDiscount(request.getDeliveryDiscount() != null ? request.getDeliveryDiscount() : BigDecimal.ZERO);
            delivery.setItemDiscount(itemPayload.getItemDiscount() != null ? itemPayload.getItemDiscount() : BigDecimal.ZERO);
            delivery.setOrderDiscount(request.getOrderDiscount() != null ? request.getOrderDiscount() : BigDecimal.ZERO);
            delivery.setActualDeliveryCost(request.getActualDeliveryCost() != null ? request.getActualDeliveryCost() : deliveryFee);
            
            delivery.setEstimatedDeliveryTime(OffsetDateTime.now().plusHours(2));

            delivery.setProduct(product);
            delivery.setAutoCreatedCompany(autoCreatedCompany);
            delivery.setAutoCreatedDriver(autoCreatedDriver);
            delivery.setAutoCreatedReceiver(autoCreatedReceiver && itemIndex == 0); // Only first item marks auto-created receiver
            delivery.setAutoCreatedProduct(autoCreatedProduct);
            delivery.setFeeAutoCalculated(true);

            // Assign the batch id so all items created in this POST share the same batch
            delivery.setBatchId(batchId);

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

        // Create batch-level delivery photos if provided (package photos)
        if (request.getDeliveryPhotos() != null && !request.getDeliveryPhotos().isEmpty() && firstDelivery != null) {
            int startIndex = 1000; // Use high index to separate from item photos
            for (int i = 0; i < request.getDeliveryPhotos().size(); i++) {
                DeliveryPhoto photo = new DeliveryPhoto(firstDelivery, request.getDeliveryPhotos().get(i), startIndex + i);
                deliveryPhotoRepository.save(photo);
            }
            log.info("Created {} delivery/package photos", request.getDeliveryPhotos().size());
        }

        log.info("Batch delivery creation completed with {} items", request.getItems().size());
        return firstDelivery;
    }

    @Transactional
    public java.util.List<DeliveryItem> appendItemsToBatch(User sender, java.util.UUID batchId,
            java.util.List<com.delivery.deliveryapi.controller.DeliveryController.DeliveryItemPayload> items) {
        if (batchId == null) throw new IllegalArgumentException("batchId is required");

        // Ensure package exists and is appendable
        var optPkg = deliveryPackageRepository.findById(batchId);
        if (optPkg.isEmpty()) {
            throw new IllegalArgumentException("Batch not found: " + batchId);
        }
        DeliveryPackage pkg = optPkg.get();
        if (pkg.getStatus() != DeliveryPackageStatus.CREATED && pkg.getStatus() != DeliveryPackageStatus.AWAITING_PICKUP) {
            throw new IllegalArgumentException("Cannot append items to package in status: " + pkg.getStatus());
        }

        java.util.List<DeliveryItem> existing = deliveryItemRepository.findByBatchId(batchId);
        if (existing == null || existing.isEmpty()) {
            throw new IllegalArgumentException("No existing items found for batch: " + batchId);
        }

        // Use first item as context
        DeliveryItem context = existing.get(0);

        // Only allow append if sender is the original sender or in same company
        if (!sender.getId().equals(context.getSender().getId())) {
            if (sender.getCompany() == null || context.getSender().getCompany() == null ||
                !sender.getCompany().getId().equals(context.getSender().getCompany().getId())) {
                throw new IllegalArgumentException("Not allowed to append items to this batch");
            }
        }

        java.util.List<DeliveryItem> created = new java.util.ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            var itemPayload = items.get(i);

            // reuse product resolution logic from createDelivery (copying simplified logic)
            Product product = null;
            boolean autoCreatedProduct = false;

            if (itemPayload.getProductId() != null) {
                Optional<Product> existingProduct = productRepository.findById(itemPayload.getProductId());
                if (existingProduct.isPresent()) {
                    product = existingProduct.get();
                    if (!product.getCompany().getId().equals(sender.getCompany().getId())) {
                        throw new IllegalArgumentException("Access denied: Product belongs to different company");
                    }
                    product.setUsageCount(product.getUsageCount() + 1);
                    product.setLastUsedAt(java.time.OffsetDateTime.now());
                    productRepository.save(product);
                } else {
                    throw new IllegalArgumentException("Product not found: " + itemPayload.getProductId());
                }
            } else if (itemPayload.getProductName() != null && !itemPayload.getProductName().trim().isEmpty()) {
                String searchName = itemPayload.getProductName().trim();
                java.util.List<Product> existingProducts = productRepository.searchProductsByName(sender.getCompany().getId(), searchName);
                if (!existingProducts.isEmpty()) {
                    product = existingProducts.get(0);
                    product.setUsageCount(product.getUsageCount() + 1);
                    product.setLastUsedAt(java.time.OffsetDateTime.now());
                    productRepository.save(product);
                } else {
                    product = productService.createProductFromDelivery(sender, itemPayload.getProductName(), itemPayload.getPrice(), context.getDeliveryFee(), itemPayload.getItemPhotos());
                    productRepository.flush();
                    autoCreatedProduct = true;
                }
            } else if (itemPayload.getItemDescription() != null && !itemPayload.getItemDescription().trim().isEmpty()) {
                String fallbackName = itemPayload.getItemDescription().length() > 100 ?
                    itemPayload.getItemDescription().substring(0, 100) : itemPayload.getItemDescription();
                java.util.List<Product> existingProducts = productRepository.searchProductsByName(sender.getCompany().getId(), fallbackName);
                if (!existingProducts.isEmpty()) {
                    product = existingProducts.get(0);
                    product.setUsageCount(product.getUsageCount() + 1);
                    product.setLastUsedAt(java.time.OffsetDateTime.now());
                    productRepository.save(product);
                } else {
                    product = productService.createProductFromDelivery(sender, fallbackName, itemPayload.getPrice(), context.getDeliveryFee(), itemPayload.getItemPhotos());
                    productRepository.flush();
                    autoCreatedProduct = true;
                }
            } else {
                throw new IllegalArgumentException("Item " + (i + 1) + ": productName or itemDescription is required");
            }

            DeliveryItem delivery = new DeliveryItem();
            delivery.setSender(sender);
            delivery.setReceiver(context.getReceiver());
            delivery.setDeliveryCompany(context.getDeliveryCompany());
            delivery.setDeliveryDriver(context.getDeliveryDriver());
            delivery.setItemDescription(itemPayload.getItemDescription());
            delivery.setPaymentMethod(context.getPaymentMethod());

            delivery.setPickupAddress(context.getPickupAddress());
            delivery.setPickupProvince(context.getPickupProvince());
            delivery.setPickupDistrict(context.getPickupDistrict());

            delivery.setDeliveryAddress(context.getDeliveryAddress());
            delivery.setDeliveryProvince(context.getDeliveryProvince());
            delivery.setDeliveryDistrict(context.getDeliveryDistrict());
            delivery.setItemValue(itemPayload.getPrice());
            delivery.setQuantity(itemPayload.getQuantity());

            // IMPORTANT: Keep the original delivery fee unchanged
            delivery.setDeliveryFee(context.getDeliveryFee());
            delivery.setEstimatedDeliveryTime(context.getEstimatedDeliveryTime());

            delivery.setProduct(product);
            delivery.setAutoCreatedCompany(false);
            delivery.setAutoCreatedDriver(false);
            delivery.setAutoCreatedReceiver(false);
            delivery.setAutoCreatedProduct(autoCreatedProduct);
            delivery.setFeeAutoCalculated(context.getFeeAutoCalculated() != null ? context.getFeeAutoCalculated() : true);

            // Keep the same batch id
            delivery.setBatchId(batchId);

            delivery = deliveryItemRepository.save(delivery);
            created.add(delivery);
        }

        return created;
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
        } else if (DELIVERY_TYPE_DRIVER.equalsIgnoreCase(deliveryType)) {
            // Accept either driverPhone or companyPhone for driver deliveries (for backward compatibility)
            boolean hasDriverPhone = request.getDriverPhone() != null && !request.getDriverPhone().trim().isEmpty();
            boolean hasCompanyPhone = request.getCompanyPhone() != null && !request.getCompanyPhone().trim().isEmpty();
            
            if (!hasDriverPhone && !hasCompanyPhone) {
                throw new IllegalArgumentException("Driver phone is required for driver deliveries");
            }
            
            // If companyPhone is provided but not driverPhone, copy it over
            if (!hasDriverPhone && hasCompanyPhone) {
                request.setDriverPhone(request.getCompanyPhone());
            }
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