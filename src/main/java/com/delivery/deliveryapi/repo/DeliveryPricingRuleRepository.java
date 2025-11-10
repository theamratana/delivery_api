package com.delivery.deliveryapi.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delivery.deliveryapi.model.DeliveryPricingRule;
import com.delivery.deliveryapi.model.Company;

@Repository
public interface DeliveryPricingRuleRepository extends JpaRepository<DeliveryPricingRule, UUID> {

    List<DeliveryPricingRule> findByCompanyAndIsActiveOrderByPriorityDesc(Company company, Boolean isActive);

    @Cacheable(value = "pricingRules", key = "#company.id + '_' + #province + '_' + #district")
    @Query("SELECT r FROM DeliveryPricingRule r WHERE r.company = :company AND r.isActive = true " +
           "AND (r.province IS NULL OR r.province = :province) " +
           "AND (r.district IS NULL OR r.district = :district) " +
           "ORDER BY r.priority DESC, " +
           "CASE WHEN r.province = :province AND r.district = :district THEN 3 " +
           "WHEN r.province = :province AND r.district IS NULL THEN 2 " +
           "WHEN r.province IS NULL AND r.district IS NULL THEN 1 " +
           "ELSE 0 END DESC")
    List<DeliveryPricingRule> findApplicableRules(@Param("company") Company company,
                                                 @Param("province") String province,
                                                 @Param("district") String district);

    Optional<DeliveryPricingRule> findByCompanyAndProvinceAndDistrictAndIsActive(Company company, String province, String district, Boolean isActive);

    List<DeliveryPricingRule> findByCompanyAndIsActive(Company company, Boolean isActive);
}