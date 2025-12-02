package com.delivery.deliveryapi.controller;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.delivery.deliveryapi.model.DeliveryItem;
import com.delivery.deliveryapi.model.DeliveryStatus;
import com.delivery.deliveryapi.model.DeliveryTracking;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserRole;
import com.delivery.deliveryapi.repo.DeliveryItemRepository;
import com.delivery.deliveryapi.repo.DeliveryTrackingRepository;
import com.delivery.deliveryapi.repo.UserRepository;

class DeliveryControllerChangeStatusTest {

    @Mock
    private DeliveryItemRepository deliveryItemRepository;

    @Mock
    private DeliveryTrackingRepository deliveryTrackingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DeliveryController deliveryController;

    private UUID userId;
    private UUID deliveryId;
    private User driverUser;
    private DeliveryItem deliveryItem;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        deliveryId = UUID.randomUUID();

        driverUser = new User();
        Field uidField = User.class.getDeclaredField("id");
        uidField.setAccessible(true);
        uidField.set(driverUser, userId);
        driverUser.setUsername("driver1");
        driverUser.setUserRole(UserRole.DRIVER);

        deliveryItem = new DeliveryItem();
        Field didField = DeliveryItem.class.getDeclaredField("id");
        didField.setAccessible(true);
        didField.set(deliveryItem, deliveryId);
        deliveryItem.setDeliveryDriver(driverUser);
        deliveryItem.setStatus(DeliveryStatus.OUT_FOR_DELIVERY);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId.toString());
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testDriverCanChangeStatusToDelivered() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(driverUser));
        when(deliveryItemRepository.findById(deliveryId)).thenReturn(Optional.of(deliveryItem));

        DeliveryController.UpdateStatusRequest req = new DeliveryController.UpdateStatusRequest();
        req.setStatus("DELIVERED");
        req.setNote("Delivered to customer");

        ResponseEntity<?> resp = deliveryController.changeDeliveryStatus(deliveryId, req);
        assertTrue(resp.getStatusCode().is2xxSuccessful());

        @SuppressWarnings("unchecked")
        var body = (java.util.Map<String, Object>) resp.getBody();
        assertEquals("DELIVERED", body.get("status"));
        assertEquals(deliveryId, body.get("deliveryId"));

        // Note should be stored on the delivery item
        assertEquals("Delivered to customer", deliveryItem.getLastStatusNote());

        // Verify that save was called for delivery and tracking
        verify(deliveryItemRepository).save(deliveryItem);
        verify(deliveryTrackingRepository).save(org.mockito.ArgumentMatchers.any(DeliveryTracking.class));
    }
}
