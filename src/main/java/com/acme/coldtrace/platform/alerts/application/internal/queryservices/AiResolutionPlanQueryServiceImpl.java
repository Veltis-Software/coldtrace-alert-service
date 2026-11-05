package com.acme.coldtrace.platform.alerts.application.internal.queryservices;

import com.acme.coldtrace.platform.alerts.application.queryservices.AiResolutionPlanQueryFailure;
import com.acme.coldtrace.platform.alerts.application.queryservices.AiResolutionPlanQueryService;
import com.acme.coldtrace.platform.alerts.domain.model.aggregates.AiResolutionPlan;
import com.acme.coldtrace.platform.alerts.domain.model.queries.GetAiResolutionPlansByIncidentIdAndOrganizationIdQuery;
import com.acme.coldtrace.platform.alerts.domain.repositories.AiResolutionPlanRepository;
import com.acme.coldtrace.platform.alerts.application.ports.IncidentEvidenceReader;
import com.acme.coldtrace.platform.iam.interfaces.acl.IamContextFacade;
import com.acme.coldtrace.platform.shared.application.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service implementation for AI resolution plan history queries.
 *
 * @since 1.0
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class AiResolutionPlanQueryServiceImpl implements AiResolutionPlanQueryService {
    private final AiResolutionPlanRepository aiResolutionPlanRepository;
    private final IncidentEvidenceReader incidentRepository;
    private final IamContextFacade iamContextFacade;

    public AiResolutionPlanQueryServiceImpl(
            AiResolutionPlanRepository aiResolutionPlanRepository,
            IncidentEvidenceReader incidentRepository,
            IamContextFacade iamContextFacade
    ) {
        this.aiResolutionPlanRepository = aiResolutionPlanRepository;
        this.incidentRepository = incidentRepository;
        this.iamContextFacade = iamContextFacade;
    }

    /**
     * Retrieves plan history for one organization-scoped incident.
     *
     * @param query query object containing organization and incident identifiers
     * @return success with plan history ordered latest first or failure with query error
     */
    @Override
    public Result<List<AiResolutionPlan>, AiResolutionPlanQueryFailure> handle(
            GetAiResolutionPlansByIncidentIdAndOrganizationIdQuery query
    ) {
        if (!iamContextFacade.organizationExists(query.organizationId())) {
            log.warn("Organization not found for AI resolution plan history: organizationId={}",
                    query.organizationId());
            return Result.failure(new AiResolutionPlanQueryFailure.OrganizationNotFound());
        }
        if (!incidentRepository.existsByIdAndOrganizationId(query.incidentId(), query.organizationId())) {
            log.warn("Incident not found for AI resolution plan history: organizationId={}, incidentId={}",
                    query.organizationId(), query.incidentId());
            return Result.failure(new AiResolutionPlanQueryFailure.IncidentNotFound());
        }

        var plans = aiResolutionPlanRepository.findAllByIncidentIdAndOrganizationId(
                query.incidentId(),
                query.organizationId()
        );
        log.debug("Found {} AI resolution plans for organizationId={}, incidentId={}",
                plans.size(), query.organizationId(), query.incidentId());
        return Result.success(plans);
    }
}
