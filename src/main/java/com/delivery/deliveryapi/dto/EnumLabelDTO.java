package com.delivery.deliveryapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for returning enum values with labels to frontend.
 * Supports multi-language labels.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnumLabelDTO {
    private String value;
    private String name;
    private String khmerName;

    /**
     * Get label by language code.
     * @param lang "en" or "km"
     * @return Localized label
     */
    public String getLabel(String lang) {
        return "km".equalsIgnoreCase(lang) ? khmerName : name;
    }
}
