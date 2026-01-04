package com.delivery.deliveryapi.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.controller.base.BaseApiController;
import com.delivery.deliveryapi.model.DeliveryFee;
import com.delivery.deliveryapi.repo.UserRepository;
import com.delivery.deliveryapi.service.DeliveryFeeService;

/**
 * DeliveryFee API Controller - RESTful endpoints
 * 
 * Automatically provides:
 * - GET    /api/delivery-fees - List all (with pagination and filters)
 *          Examples: ?page=0&size=20&sort=fee,desc
 *                   ?provinceId=xxx&targetCompanyId=yyy&active=true
 *                   ?min_fee=2.0&max_fee=10.0
 *                   ?from_createdAt=2026-01-01&to_createdAt=2026-12-31
 * - GET    /api/delivery-fees/{id} - Get delivery fee by ID
 * - POST   /api/delivery-fees - Create new delivery fee
 * - PUT    /api/delivery-fees/{id} - Update delivery fee
 * - DELETE /api/delivery-fees/{id} - Soft delete (default, sets deleted flag)
 * - DELETE /api/delivery-fees/{id}?hardDelete=true - Hard delete (removes from DB permanently)
 * - DELETE /api/delivery-fees?ids=uuid1,uuid2 - Batch soft delete
 * - DELETE /api/delivery-fees?ids=uuid1,uuid2&hardDelete=true - Batch hard delete
 * 
 * All endpoints require JWT authentication
 * Filters are applied BEFORE pagination
 */
@RestController
@RequestMapping("/delivery-fees")
public class DeliveryFeeController extends BaseApiController<DeliveryFee> {

    public DeliveryFeeController(DeliveryFeeService service, UserRepository userRepository) {
        super(service, userRepository);
    }
}
