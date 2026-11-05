package com.acme.coldtrace.platform.alerts.infrastructure.persistence.jpa.repositories;

import com.acme.coldtrace.platform.alerts.infrastructure.persistence.jpa.entities.AiResolutionPlanPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for AI resolution plan persistence entities.
 *
 * @since 1.0
 */
@Repository
public interface AiResolutionPlanPersistenceRepository extends JpaRepository<AiResolutionPlanPersistenceEntity, Long> {
    List<AiResolutionPlanPersistenceEntity> findAllByIncidentIdAndOrganizationIdOrderByGeneratedAtDescIdDesc(
            Long incidentId,
            Long organizationId
    );

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<AiResolutionPlanPersistenceEntity> findByIdAndIncidentIdAndOrganizationId(
            Long id,
            Long incidentId,
            Long organizationId
    );
}
