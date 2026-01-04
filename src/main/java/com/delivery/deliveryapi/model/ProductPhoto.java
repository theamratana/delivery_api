package com.delivery.deliveryapi.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "product_photos")
public class ProductPhoto {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "photo_index", nullable = false)
    private Integer photoIndex;

    @Column(name = "photo_url", nullable = false, length = 512)
    private String photoUrl;

    // Constructors
    public ProductPhoto() {}

    public ProductPhoto(Product product, String photoUrl, Integer photoIndex) {
        this.product = product;
        this.photoUrl = photoUrl;
        this.photoIndex = photoIndex;
    }
}
