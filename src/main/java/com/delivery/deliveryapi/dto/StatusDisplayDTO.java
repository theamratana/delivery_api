package com.delivery.deliveryapi.dto;

import com.delivery.deliveryapi.model.DeliveryStatus;
import com.delivery.deliveryapi.model.StatusGroup;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for displaying status information with translations and grouping
 */
public class StatusDisplayDTO {

    @JsonProperty("code")
    private String code;

    @JsonProperty("english")
    private String english;

    @JsonProperty("khmer")
    private String khmer;

    @JsonProperty("group")
    private StatusGroupDisplayDTO group;

    public StatusDisplayDTO() {
    }

    public StatusDisplayDTO(String code, String english, String khmer, StatusGroupDisplayDTO group) {
        this.code = code;
        this.english = english;
        this.khmer = khmer;
        this.group = group;
    }

    public static StatusDisplayDTO fromDeliveryStatus(DeliveryStatus status) {
        if (status == null) {
            return null;
        }
        return new StatusDisplayDTO(
            status.name(),
            status.getEnglishName(),
            status.getKhmerName(),
            StatusGroupDisplayDTO.fromStatusGroup(status.getGroup())
        );
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    public String getKhmer() {
        return khmer;
    }

    public void setKhmer(String khmer) {
        this.khmer = khmer;
    }

    public StatusGroupDisplayDTO getGroup() {
        return group;
    }

    public void setGroup(StatusGroupDisplayDTO group) {
        this.group = group;
    }

    /**
     * Inner DTO for status group information
     */
    public static class StatusGroupDisplayDTO {

        @JsonProperty("code")
        private String code;

        @JsonProperty("english")
        private String english;

        @JsonProperty("khmer")
        private String khmer;

        public StatusGroupDisplayDTO() {
        }

        public StatusGroupDisplayDTO(String code, String english, String khmer) {
            this.code = code;
            this.english = english;
            this.khmer = khmer;
        }

        public static StatusGroupDisplayDTO fromStatusGroup(StatusGroup group) {
            if (group == null) {
                return null;
            }
            return new StatusGroupDisplayDTO(
                group.name(),
                group.getEnglishName(),
                group.getKhmerName()
            );
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getEnglish() {
            return english;
        }

        public void setEnglish(String english) {
            this.english = english;
        }

        public String getKhmer() {
            return khmer;
        }

        public void setKhmer(String khmer) {
            this.khmer = khmer;
        }
    }
}
