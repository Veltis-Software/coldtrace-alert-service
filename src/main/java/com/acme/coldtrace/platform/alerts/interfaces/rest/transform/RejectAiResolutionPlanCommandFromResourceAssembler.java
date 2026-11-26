package com.acme.coldtrace.platform.alerts.interfaces.rest.transform;

import com.acme.coldtrace.platform.alerts.domain.model.commands.RejectAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.RejectAiResolutionPlanResource;

/**
 * Assembler that converts rejection request resources into AI resolution plan commands.
 *
 * @since 1.0
 */
public final class RejectAiResolutionPlanCommandFromResourceAssembler {
    private RejectAiResolutionPlanCommandFromResourceAssembler() {
    }

    /**
     * Converts a rejection request into an application command.
     *
     * @param resource rejection request resource
     * @param organizationId organization identifier from the path
     * @param incidentId incident identifier from the path
     * @param planId plan identifier from the path
     * @return rejection command
     */
    public static RejectAiResolutionPlanCommand toCommandFromResource(
            RejectAiResolutionPlanResource resource,
            Long organizationId,
            Long incidentId,
            Long planId
    ) {
        return new RejectAiResolutionPlanCommand(
                organizationId,
                incidentId,
                planId,
                resource.rejectedBy(),
                resource.rejectionReason()
        );
    }
}
