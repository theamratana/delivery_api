package com.delivery.deliveryapi.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.delivery.deliveryapi.service.ImageService;
import com.delivery.deliveryapi.dto.ImageUploadResult;

@RestController
@RequestMapping("/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<List<ImageUploadResult>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        try {
            // Get current user from security context (for authentication check)
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof String)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Upload images - get user id
            String userId = null;
            if (auth != null && auth.getPrincipal() instanceof String) {
                userId = (String) auth.getPrincipal();
            }
            List<ImageUploadResult> imageResults = imageService.uploadImages(files, userId);
            return ResponseEntity.ok(imageResults);

        } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of(new ImageUploadResult(null, "Validation error: " + e.getMessage())));
        } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(new ImageUploadResult(null, "Error: " + e.getMessage())));
        }
    }
}