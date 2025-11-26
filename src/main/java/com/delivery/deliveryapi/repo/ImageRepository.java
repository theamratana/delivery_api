package com.delivery.deliveryapi.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery.deliveryapi.model.Image;

public interface ImageRepository extends JpaRepository<Image, UUID> {
    Optional<Image> findByUrl(String url);
}
