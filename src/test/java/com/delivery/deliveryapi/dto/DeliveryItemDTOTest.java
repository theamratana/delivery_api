package com.delivery.deliveryapi.dto;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.delivery.deliveryapi.model.DeliveryItem;

class DeliveryItemDTOTest {

    @Test
    void convertsLastStatusNote() throws Exception {
        DeliveryItem item = new DeliveryItem();
        Field idField = DeliveryItem.class.getDeclaredField("id");
        idField.setAccessible(true);
        UUID id = UUID.randomUUID();
        idField.set(item, id);

        item.setItemDescription("Test item");
        item.setLastStatusNote("Left at front door");

        DeliveryItemDTO dto = DeliveryItemDTO.fromDeliveryItem(item);
        assertEquals("Left at front door", dto.getLastStatusNote());
    }
}
