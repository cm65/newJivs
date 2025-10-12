package com.jivs.platform.repository;

import com.jivs.platform.domain.businessobject.BusinessObjectDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BusinessObjectDefinition entity
 */
@Repository
public interface BusinessObjectDefinitionRepository extends JpaRepository<BusinessObjectDefinition, Long> {

    Optional<BusinessObjectDefinition> findByTechnicalName(String technicalName);

    Boolean existsByTechnicalName(String technicalName);

    List<BusinessObjectDefinition> findByObjectType(BusinessObjectDefinition.BusinessObjectType objectType);

    List<BusinessObjectDefinition> findBySourceSystem(String sourceSystem);

    List<BusinessObjectDefinition> findByIsActiveTrue();

    Page<BusinessObjectDefinition> findByIsActive(Boolean isActive, Pageable pageable);

    @Query("SELECT b FROM BusinessObjectDefinition b LEFT JOIN FETCH b.fields WHERE b.technicalName = :technicalName")
    Optional<BusinessObjectDefinition> findByTechnicalNameWithFields(@Param("technicalName") String technicalName);

    @Query("SELECT b FROM BusinessObjectDefinition b WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.technicalName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<BusinessObjectDefinition> searchByNameOrTechnicalName(@Param("search") String search, Pageable pageable);
}