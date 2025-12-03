package com.delivery.deliveryapi.controller;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import com.delivery.deliveryapi.model.DeliveryStatus;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserRole;
import com.delivery.deliveryapi.repo.DeliveryItemRepository;
import com.delivery.deliveryapi.repo.UserRepository;

class DeliveryControllerSummaryTest {

    @Mock
    private DeliveryItemRepository deliveryItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DeliveryController deliveryController;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();

        testUser = new User();
        Field userIdField = User.class.getDeclaredField("id");
        userIdField.setAccessible(true);
        userIdField.set(testUser, userId);
        testUser.setUsername("testuser");
        testUser.setUserRole(UserRole.OWNER);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId.toString());
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testSummaryEndpointReturnsCounts() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { DeliveryStatus.CREATED, 10L });
        rows.add(new Object[] { DeliveryStatus.DELIVERED, 20L });

        when(deliveryItemRepository.countStatusByUserInRange(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(rows);

        DeliveryController.SummaryRequest req = new DeliveryController.SummaryRequest();
        req.setStartDate(OffsetDateTime.now().minusDays(7).toString());
        req.setEndDate(OffsetDateTime.now().toString());

        ResponseEntity<?> resp = deliveryController.getDeliverySummaryByStatus(req);
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        assertTrue(resp.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertTrue(body.containsKey("counts"));
        assertTrue(body.containsKey("groups"));

        @SuppressWarnings("unchecked")
        Map<String, Number> counts = (Map<String, Number>) body.get("counts");
        assertEquals(10L, counts.get("CREATED").longValue());
        assertEquals(20L, counts.get("DELIVERED").longValue());

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Number>> groups = (Map<String, Map<String, Number>>) body.get("groups");
        // CREATED should belong to Sender group
        assertTrue(groups.get("Sender").containsKey("CREATED"));
        assertEquals(10L, groups.get("Sender").get("CREATED").longValue());
        // DELIVERED should belong to Receiver group
        assertTrue(groups.get("Receiver").containsKey("DELIVERED"));
        assertEquals(20L, groups.get("Receiver").get("DELIVERED").longValue());
    }
}
