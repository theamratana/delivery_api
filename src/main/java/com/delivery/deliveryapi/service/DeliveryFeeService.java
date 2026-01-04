package com.delivery.deliveryapi.service;

import org.springframework.stereotype.Service;

import com.delivery.deliveryapi.model.DeliveryFee;
import com.delivery.deliveryapi.repo.DeliveryFeeRepository;
import com.delivery.deliveryapi.service.base.BaseServiceImpl;

/**
 * DeliveryFee service extending base implementation.
 * All CRUD operations inherited from BaseServiceImpl.
 * Filtering handled automatically by BaseServiceImpl.query()
 */
@Service
public class DeliveryFeeService extends BaseServiceImpl<DeliveryFee, DeliveryFeeRepository> {

    public DeliveryFeeService(DeliveryFeeRepository repository) {
        super(repository);
    }
}
