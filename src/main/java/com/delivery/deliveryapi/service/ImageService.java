package com.delivery.deliveryapi.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.delivery.deliveryapi.model.Image;
import com.delivery.deliveryapi.dto.ImageUploadResult;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.ImageRepository;
import com.delivery.deliveryapi.repo.UserRepository;

@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private static final String UPLOAD_DIR = "uploads/images/";
    private static final float JPEG_COMPRESSION_QUALITY = 0.8f; // 80% quality
    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024; // 2MB
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;

    public ImageService(ImageRepository imageRepository, UserRepository userRepository) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
    }

    public List<ImageUploadResult> uploadImages(MultipartFile[] files, String uploaderUserId) {
        List<ImageUploadResult> imageResults = new ArrayList<>();

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory", e);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Validate file type (basic check)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("Only image files are allowed");
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // Save and compress file
            Path filePath = uploadPath.resolve(filename);
            try {
                saveCompressedImage(file, filePath, contentType);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save file: " + filename, e);
            }

            // Add to URLs list (relative path)
            String url = "/" + UPLOAD_DIR + filename;
            imageResults.add(new ImageUploadResult(null, url));

            // Persist image metadata with uploader and company (if exists)
            try {
                Optional<User> optUser = Optional.empty();
                if (uploaderUserId != null) {
                    try { optUser = userRepository.findById(UUID.fromString(uploaderUserId)); } catch (Exception ignored) { log.debug("Invalid uploader id: {}", uploaderUserId); }
                }
                Image image = new Image();
                image.setUrl(url);
                if (optUser.isPresent()) {
                    User uploader = optUser.get();
                    image.setUploader(uploader);
                    if (uploader.getCompany() != null) image.setCompany(uploader.getCompany());
                }
                Image saved = imageRepository.save(image);
                // update created results with id
                for (int i = 0; i < imageResults.size(); i++) {
                    ImageUploadResult r = imageResults.get(i);
                    if (r.getUrl().equals(url) && (r.getId() == null || r.getId().isEmpty())) {
                        r.setId(saved.getId().toString());
                        break;
                    }
                }
            } catch (Exception e) {
                // Persisting metadata should not break upload flow. Just log.
                log.warn("Failed to save image metadata", e);
            }
        }

        return imageResults;
    }

    private void saveCompressedImage(MultipartFile file, Path filePath, String contentType) throws IOException {
        // For large files or JPEG, compress
        if (file.getSize() > MAX_FILE_SIZE_BYTES || JPEG_CONTENT_TYPE.equals(contentType)) {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image != null) {
                // Compress JPEG
                if (JPEG_CONTENT_TYPE.equals(contentType)) {
                    compressJpeg(image, filePath);
                } else {
                    // For other formats, just save (PNG doesn't compress well)
                    ImageIO.write(image, getFormatName(contentType), filePath.toFile());
                }
            } else {
                // Fallback to direct copy if ImageIO can't read
                Files.copy(file.getInputStream(), filePath);
            }
        } else {
            // Small files, save as-is
            Files.copy(file.getInputStream(), filePath);
        }
    }

    private void compressJpeg(BufferedImage image, Path filePath) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_COMPRESSION_QUALITY);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(filePath.toFile())) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        }
        writer.dispose();
    }

    private String getFormatName(String contentType) {
        switch (contentType) {
            case "image/jpeg": return "jpeg";
            case "image/png": return "png";
            case "image/gif": return "gif";
            case "image/webp": return "webp";
            default: return "jpeg"; // fallback
        }
    }
}