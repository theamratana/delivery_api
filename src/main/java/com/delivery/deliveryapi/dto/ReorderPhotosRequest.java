package com.delivery.deliveryapi.dto;

import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class ReorderPhotosRequest {
    
    private List<PhotoOrder> photos;
    
    @Data
    public static class PhotoOrder {
        private UUID photoId;
        private Integer newIndex;
    }
}
