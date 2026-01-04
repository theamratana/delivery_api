package com.delivery.deliveryapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResult {
    @JsonProperty("id")
    private String id;
    @JsonProperty("url")
    private String url;
}
