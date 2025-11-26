package com.delivery.deliveryapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImageUploadResult {
    @JsonProperty("id")
    private String id;
    @JsonProperty("url")
    private String url;
    public ImageUploadResult() {}
    public ImageUploadResult(String id, String url) { this.id = id; this.url = url; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
