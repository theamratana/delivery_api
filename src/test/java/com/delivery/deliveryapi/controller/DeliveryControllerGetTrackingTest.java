package com.delivery.deliveryapi.controller;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import com.delivery.deliveryapi.repo.DeliveryItemRepository;
import com.delivery.deliveryapi.repo.DeliveryTrackingRepository;
import com.delivery.deliveryapi.repo.UserRepository;

class DeliveryControllerGetTrackingTest {

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
    private User user;
    private DeliveryItem item;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();
        deliveryId = UUID.randomUUID();

        user = new User();
        Field fid = User.class.getDeclaredField("id");
        fid.setAccessible(true);
        fid.set(user, userId);

        item = new DeliveryItem();
        Field did = DeliveryItem.class.getDeclaredField("id");
        did.setAccessible(true);
        did.set(item, deliveryId);
        item.setSender(user);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId.toString());
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetTrackingAllowed() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(deliveryItemRepository.findById(deliveryId)).thenReturn(Optional.of(item));

        DeliveryTracking t1 = new DeliveryTracking(item, DeliveryStatus.CREATED, "Created", user);
        DeliveryTracking t2 = new DeliveryTracking(item, DeliveryStatus.OUT_FOR_DELIVERY, "Out for delivery", user);
        when(deliveryTrackingRepository.findByDeliveryItemIdAndDeletedFalseOrderByTimestampDesc(deliveryId)).thenReturn(List.of(t2, t1));

        ResponseEntity<?> resp = deliveryController.getDeliveryTracking(deliveryId);
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        assertTrue(resp.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        var list = (List<?>) resp.getBody();
        assertEquals(2, list.size());
    }
}
