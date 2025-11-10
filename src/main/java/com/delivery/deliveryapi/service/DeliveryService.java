package com.delivery.deliveryapi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.deliveryapi.controller.DeliveryController.CreateDeliveryRequest;
import com.delivery.deliveryapi.model.Company;
import com.delivery.deliveryapi.model.DeliveryItem;
import com.delivery.deliveryapi.model.DeliveryPhoto;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserType;
import com.delivery.deliveryapi.repo.CompanyRepository;
import com.delivery.deliveryapi.repo.DeliveryItemRepository;
import com.delivery.deliveryapi.repo.DeliveryPhotoRepository;
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

    public DeliveryService(DeliveryItemRepository deliveryItemRepository,
                          UserRepository userRepository,
                          CompanyRepository companyRepository,
                          DeliveryPhotoRepository deliveryPhotoRepository,
                          DeliveryPricingService deliveryPricingService) {
        this.deliveryItemRepository = deliveryItemRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.deliveryPhotoRepository = deliveryPhotoRepository;
        this.deliveryPricingService = deliveryPricingService;
    }

    @Transactional
    public DeliveryItem createDelivery(User sender, CreateDeliveryRequest request) {
        log.info("Creating delivery for sender: {} to receiver phone: {}", sender.getId(), request.getReceiverPhone());

        // Validate request
        validateCreateDeliveryRequest(request);

        // Find or create receiver
        User receiver = findOrCreateReceiver(request.getReceiverPhone(), request.getReceiverName());
        log.info("Using receiver: {} ({})", receiver.getId(), receiver.getPhoneE164());

        // Find or create delivery company/driver
        Company deliveryCompany = null;
        User deliveryDriver = null;

        if (DELIVERY_TYPE_COMPANY.equalsIgnoreCase(request.getDeliveryType())) {
            deliveryCompany = findOrCreateCompany(request.getCompanyName());
            log.info("Using delivery company: {} ({})", deliveryCompany.getId(), deliveryCompany.getName());
        } else if (DELIVERY_TYPE_DRIVER.equalsIgnoreCase(request.getDeliveryType())) {
            deliveryDriver = findOrCreateDriver(request.getDriverPhone());
            log.info("Using delivery driver: {} ({})", deliveryDriver.getId(), deliveryDriver.getPhoneE164());
        }

        // Calculate delivery fee using user's pricing rules
        BigDecimal deliveryFee = deliveryPricingService.calculateDeliveryFee(sender, request);

        // Create delivery item
        DeliveryItem delivery = new DeliveryItem();
        delivery.setSender(sender);
        delivery.setReceiver(receiver);
        delivery.setDeliveryCompany(deliveryCompany);
        delivery.setDeliveryDriver(deliveryDriver);
        delivery.setItemDescription(request.getItemDescription());
        delivery.setPickupAddress(request.getPickupAddress());
        delivery.setPickupProvince(request.getPickupProvince());
        delivery.setPickupDistrict(request.getPickupDistrict());
        delivery.setDeliveryAddress(request.getDeliveryAddress());
        delivery.setDeliveryProvince(request.getDeliveryProvince());
        delivery.setDeliveryDistrict(request.getDeliveryDistrict());
        delivery.setItemValue(request.getEstimatedValue());
        delivery.setDeliveryFee(deliveryFee);
        delivery.setEstimatedDeliveryTime(OffsetDateTime.now().plusHours(2)); // Default 2 hours

        delivery = deliveryItemRepository.save(delivery);
        log.info("Created delivery item: {}", delivery.getId());

        // Create delivery photos if provided
        if (request.getItemPhotos() != null && !request.getItemPhotos().isEmpty()) {
            for (int i = 0; i < request.getItemPhotos().size(); i++) {
                DeliveryPhoto photo = new DeliveryPhoto(delivery, request.getItemPhotos().get(i), i);
                deliveryPhotoRepository.save(photo);
            }
            log.info("Created {} delivery photos", request.getItemPhotos().size());
        }

        return delivery;
    }

    private void validateCreateDeliveryRequest(CreateDeliveryRequest request) {
        validateRequiredFields(request);
        validateDeliveryType(request);
    }

    private void validateRequiredFields(CreateDeliveryRequest request) {
        if (request.getReceiverPhone() == null || request.getReceiverPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Receiver phone is required");
        }
        if (request.getItemDescription() == null || request.getItemDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Item description is required");
        }
        if (request.getPickupAddress() == null || request.getPickupAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Pickup address is required");
        }
        if (request.getPickupProvince() == null || request.getPickupProvince().trim().isEmpty()) {
            throw new IllegalArgumentException("Pickup province is required");
        }
        if (request.getPickupDistrict() == null || request.getPickupDistrict().trim().isEmpty()) {
            throw new IllegalArgumentException("Pickup district is required");
        }
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