package com.delivery.deliveryapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Add /api prefix to all controller endpoints
        configurer.addPathPrefix("/api", c -> c.isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded images
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}