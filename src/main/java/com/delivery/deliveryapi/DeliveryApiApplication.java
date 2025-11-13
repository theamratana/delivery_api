package com.delivery.deliveryapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.delivery.deliveryapi.service.AdminUserService;
import com.delivery.deliveryapi.service.ProductCategoryService;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableJpaAuditing
@EnableCaching
@EnableAsync
public class DeliveryApiApplication {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryApiApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DeliveryApiApplication.class, args);
    }

    @Bean
    public CommandLineRunner initializeDefaultData(ProductCategoryService productCategoryService, AdminUserService adminUserService) {
        return args -> {
            try {
                productCategoryService.initializeDefaultCategories();
                
                // Create default system administrator if it doesn't exist
                adminUserService.createSystemAdministrator(
                    "Admin", 
                    "adminDelivery2025", 
                    "System Administrator"
                );
            } catch (Exception e) {
                logger.error("Failed to initialize default data", e);
                // Don't fail startup if this fails
            }
        };
    }

}
