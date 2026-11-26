package com.acme.coldtrace.platform.alerts.interfaces.rest.transform;

import com.acme.coldtrace.platform.alerts.domain.model.commands.ApproveAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.ApproveAiResolutionPlanResource;

/**
 * Assembler that converts approval request resources into AI resolution plan commands.
 *
 * @since 1.0
 */
public final class ApproveAiResolutionPlanCommandFromResourceAssembler {
    private ApproveAiResolutionPlanCommandFromResourceAssembler() {
    }

    /**
     * Converts an approval request into an application command.
     *
     * @param resource approval request resource
     * @param organizationId organization identifier from the path
     * @param incidentId incident identifier from the path
     * @param planId plan identifier from the path
     * @return approval command
     */
    public static ApproveAiResolutionPlanCommand toCommandFromResource(
            ApproveAiResolutionPlanResource resource,
            Long organizationId,
            Long incidentId,
            Long planId
    ) {
        return new ApproveAiResolutionPlanCommand(
                organizationId,
                incidentId,
                planId,
                resource.approvedBy(),
                resource.finalCorrectiveAction(),
                resource.finalResolutionNotes()
        );
    }
}
