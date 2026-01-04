package com.delivery.deliveryapi.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delivery.deliveryapi.model.DeliveryFee;

public interface DeliveryFeeRepository extends JpaRepository<DeliveryFee, UUID>, JpaSpecificationExecutor<DeliveryFee> {
    
    // /**
    //  * Find all delivery fees managed by a company
    //  */
    // @Query("SELECT df FROM DeliveryFee df WHERE df.companyId = :companyId AND df.active = true")
    // Page<DeliveryFee> findByCompanyId(@Param("companyId") UUID companyId, Pageable pageable);
    
    // /**
    //  * Find delivery fees for a target company
    //  */
    // @Query("SELECT df FROM DeliveryFee df WHERE df.targetCompanyId = :targetCompanyId AND df.active = true")
    // List<DeliveryFee> findByTargetCompanyId(@Param("targetCompanyId") UUID targetCompanyId);
    
    // /**
    //  * Find delivery fee with hierarchical fallback
    //  * Priority: district > province > default (null, null)
    //  */
    // @Query("""
    //     SELECT df FROM DeliveryFee df 
    //     WHERE df.targetCompanyId = :targetCompanyId 
    //     AND df.active = true
    //     AND (
    //         (df.provinceId = :provinceId AND df.districtId = :districtId)
    //         OR (df.provinceId = :provinceId AND df.districtId IS NULL)
    //         OR (df.provinceId IS NULL AND df.districtId IS NULL)
    //     )
    //     ORDER BY 
    //         CASE 
    //             WHEN df.districtId IS NOT NULL THEN 1
    //             WHEN df.provinceId IS NOT NULL THEN 2
    //             ELSE 3
    //         END
    //     """)
    // List<DeliveryFee> findWithFallback(
    //     @Param("targetCompanyId") UUID targetCompanyId,
    //     @Param("provinceId") UUID provinceId,
    //     @Param("districtId") UUID districtId
    // );
}
