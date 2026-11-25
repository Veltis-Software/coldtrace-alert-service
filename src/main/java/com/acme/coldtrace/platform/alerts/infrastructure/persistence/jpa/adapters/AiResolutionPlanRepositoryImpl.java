package com.acme.coldtrace.platform.alerts.infrastructure.persistence.jpa.adapters;

import com.acme.coldtrace.platform.alerts.domain.model.aggregates.AiResolutionPlan;
import com.acme.coldtrace.platform.alerts.domain.repositories.AiResolutionPlanRepository;
import com.acme.coldtrace.platform.alerts.infrastructure.persistence.jpa.assemblers.AiResolutionPlanPersistenceAssembler;
import com.acme.coldtrace.platform.alerts.infrastructure.persistence.jpa.repositories.AiResolutionPlanPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA-backed adapter for the AI resolution plan domain repository.
 *
 * @since 1.0
 */
@Repository
public class AiResolutionPlanRepositoryImpl implements AiResolutionPlanRepository {
    private final AiResolutionPlanPersistenceRepository aiResolutionPlanPersistenceRepository;

    public AiResolutionPlanRepositoryImpl(
            AiResolutionPlanPersistenceRepository aiResolutionPlanPersistenceRepository
    ) {
        this.aiResolutionPlanPersistenceRepository = aiResolutionPlanPersistenceRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AiResolutionPlan> findAllByIncidentIdAndOrganizationId(Long incidentId, Long organizationId) {
        return aiResolutionPlanPersistenceRepository
                .findAllByIncidentIdAndOrganizationIdOrderByGeneratedAtDescIdDesc(incidentId, organizationId)
                .stream()
                .map(AiResolutionPlanPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<AiResolutionPlan> findByIdAndIncidentIdAndOrganizationId(
            Long id,
            Long incidentId,
            Long organizationId
    ) {
        return aiResolutionPlanPersistenceRepository.findByIdAndIncidentIdAndOrganizationId(
                id,
                incidentId,
                organizationId
        ).map(AiResolutionPlanPersistenceAssembler::toDomainFromPersistence);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AiResolutionPlan save(AiResolutionPlan plan) {
        if (plan.getId() == null) {
            var entity = AiResolutionPlanPersistenceAssembler.toPersistenceFromDomain(plan);
            return AiResolutionPlanPersistenceAssembler.toDomainFromPersistence(
                    aiResolutionPlanPersistenceRepository.save(entity)
            );
        }

        var entity = aiResolutionPlanPersistenceRepository.findById(plan.getId())
                .orElseGet(() -> AiResolutionPlanPersistenceAssembler.toPersistenceFromDomain(plan));
        AiResolutionPlanPersistenceAssembler.copyDomainState(plan, entity);
        return AiResolutionPlanPersistenceAssembler.toDomainFromPersistence(
                aiResolutionPlanPersistenceRepository.save(entity)
        );
    }
}
