package com.delivery.deliveryapi.dto;

import java.util.List;
import java.util.UUID;

public class ReorderPhotosRequest {
    
    private List<PhotoOrder> photos;
    
    public static class PhotoOrder {
        private UUID photoId;
        private Integer newIndex;
        
        public UUID getPhotoId() { return photoId; }
        public void setPhotoId(UUID photoId) { this.photoId = photoId; }
        
        public Integer getNewIndex() { return newIndex; }
        public void setNewIndex(Integer newIndex) { this.newIndex = newIndex; }
    }
    
    public List<PhotoOrder> getPhotos() { return photos; }
    public void setPhotos(List<PhotoOrder> photos) { this.photos = photos; }
}
