package com.delivery.deliveryapi.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.deliveryapi.dto.EnumLabelDTO;
import com.delivery.deliveryapi.model.enums.AddressType;
import com.delivery.deliveryapi.model.enums.DeliveryStatus;
import com.delivery.deliveryapi.model.enums.DiscountType;
import com.delivery.deliveryapi.model.enums.OrderSource;
import com.delivery.deliveryapi.model.enums.OrderStatus;
import com.delivery.deliveryapi.model.enums.OrderType;
import com.delivery.deliveryapi.model.enums.PaymentStatus;
import com.delivery.deliveryapi.model.enums.PaymentType;

/**
 * Controller for exposing enum values with multi-language labels to frontend.
 * No authentication required - public reference data.
 */
@RestController
@RequestMapping("/enums")
public class EnumController {

    @GetMapping("/order-types")
    public ResponseEntity<List<EnumLabelDTO>> getOrderTypes() {
        List<EnumLabelDTO> enums = Arrays.stream(OrderType.values())
            .map(e -> new EnumLabelDTO(e.name(), e.getName(), e.getKhmerName()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(enums);
    }

    @GetMapping("/order-sources")
    public ResponseEntity<List<EnumLabelDTO>> getOrderSources() {
        List<EnumLabelDTO> enums = Arrays.stream(OrderSource.values())
            .map(e -> new EnumLabelDTO(e.name(), e.getName(), e.getKhmerName()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(enums);
    }

    @GetMapping("/payment-types")
    public ResponseEntity<List<EnumLabelDTO>> getPaymentTypes() {
        List<EnumLabelDTO> enums = Arrays.stream(PaymentType.values())
            .map(e -> new EnumLabelDTO(e.name(), e.getName(), e.getKhmerName()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(enums);
    }

    @GetMapping("/payment-statuses")
    public ResponseEntity<List<EnumLabelDTO>> getPaymentStatuses() {
        List<EnumLabelDTO> enums = Arrays.stream(PaymentStatus.values())
            .map(e -> new EnumLabelDTO(e.name(), e.getName(), e.getKhmerName()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(enums);
    }

    @GetMapping("/order-statuses")
    public ResponseEntity<List<EnumLabelDTO>> getOrderStatuses() {
        List<EnumLabelDTO> enums = Arrays.stream(OrderStatus.values())
            .map(e -> new EnumLabelDTO(e.name(), e.getName(), e.getKhmerName()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(enums);
    }

    @GetMapping("/delivery-statuses")
    public ResponseEntity<List<EnumLabelDTO>> getDeliveryStatuses() {
        List<EnumLabelDTO> enums = Arrays.stream(DeliveryStatus.values())
            .map(e -> new EnumLabelDTO(e.name(), e.getName(), e.getKhmerName()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(enums);
    }

    @GetMapping("/discount-types")
    public ResponseEntity<List<EnumLabelDTO>> getDiscountTypes() {
        List<EnumLabelDTO> enums = Arrays.stream(DiscountType.values())
            .map(e -> new EnumLabelDTO(e.name(), e.getName(), e.getKhmerName()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(enums);
    }

    @GetMapping("/address-types")
    public ResponseEntity<List<EnumLabelDTO>> getAddressTypes() {
        List<EnumLabelDTO> enums = Arrays.stream(AddressType.values())
            .map(e -> new EnumLabelDTO(e.name(), e.getName(), e.getKhmerName()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(enums);
    }
}
