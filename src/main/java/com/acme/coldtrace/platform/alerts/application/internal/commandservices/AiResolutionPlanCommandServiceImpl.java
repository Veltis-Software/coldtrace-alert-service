package com.acme.coldtrace.platform.alerts.application.internal.commandservices;

import com.acme.coldtrace.platform.aiassistance.application.commandservices.AiAssistanceCommandService;
import com.acme.coldtrace.platform.aiassistance.application.model.AiGeneratedResponse;
import com.acme.coldtrace.platform.aiassistance.application.model.IncidentResolutionPlanDraft;
import com.acme.coldtrace.platform.aiassistance.domain.model.commands.GenerateIncidentResolutionPlanDraftCommand;
import com.acme.coldtrace.platform.alerts.application.commandservices.AiResolutionPlanCommandFailure;
import com.acme.coldtrace.platform.alerts.application.commandservices.AiResolutionPlanCommandService;
import com.acme.coldtrace.platform.alerts.application.ports.IncidentResolutionPort;
import com.acme.coldtrace.platform.alerts.domain.model.aggregates.AiResolutionPlan;
import com.acme.coldtrace.platform.alerts.application.ports.IncidentEvidenceSnapshot;
import com.acme.coldtrace.platform.alerts.domain.model.commands.ApproveAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.domain.model.commands.CreateAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.domain.model.commands.GenerateAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.domain.model.commands.RejectAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.domain.model.commands.ResolveIncidentWithCorrectiveActionCommand;
import com.acme.coldtrace.platform.alerts.domain.model.valueobjects.AiResolutionPlanStep;
import com.acme.coldtrace.platform.alerts.domain.repositories.AiResolutionPlanRepository;
import com.acme.coldtrace.platform.alerts.application.ports.IncidentEvidenceReader;
import com.acme.coldtrace.platform.assetmanagement.interfaces.acl.AssetManagementContextFacade;
import com.acme.coldtrace.platform.assetmanagement.interfaces.acl.AssetManagementContextFacade.AssetSettingsSnapshot;
import com.acme.coldtrace.platform.assetmanagement.interfaces.acl.AssetManagementContextFacade.AssetSnapshot;
import com.acme.coldtrace.platform.assetmanagement.interfaces.acl.AssetManagementContextFacade.GatewaySnapshot;
import com.acme.coldtrace.platform.assetmanagement.interfaces.acl.AssetManagementContextFacade.IoTDeviceSnapshot;
import com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade;
import com.acme.coldtrace.platform.iam.interfaces.acl.IamContextFacade;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.acl.MaintenanceManagementContextFacade;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.acl.MaintenanceManagementContextFacade.MaintenanceScheduleSnapshot;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.acl.MaintenanceManagementContextFacade.TechnicalServiceRequestSnapshot;
import com.acme.coldtrace.platform.monitoring.interfaces.acl.MonitoringContextFacade;
import com.acme.coldtrace.platform.monitoring.interfaces.acl.MonitoringContextFacade.SensorReadingSnapshot;
import com.acme.coldtrace.platform.shared.application.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade.ENTITLEMENT_AI_GUIDANCE;

/**
 * Application service implementation for AI resolution plan lifecycle commands.
 *
 * @since 1.0
 */
@Slf4j
@Service
public class AiResolutionPlanCommandServiceImpl implements AiResolutionPlanCommandService {
    private static final int MAX_RECENT_READINGS = 12;
    private static final int MAX_MAINTENANCE_RECORDS = 5;

    private final AiResolutionPlanRepository aiResolutionPlanRepository;
    private final IncidentEvidenceReader incidentRepository;
    private final IncidentResolutionPort incidentCommandService;
    private final IamContextFacade iamContextFacade;
    private final AssetManagementContextFacade assetManagementContextFacade;
    private final MonitoringContextFacade monitoringContextFacade;
    private final MaintenanceManagementContextFacade maintenanceManagementContextFacade;
    private final AiAssistanceCommandService aiAssistanceCommandService;
    private final ObjectMapper jsonMapper;
    private final SubscriptionBillingContextFacade subscriptionBillingContextFacade;

    public AiResolutionPlanCommandServiceImpl(
            AiResolutionPlanRepository aiResolutionPlanRepository,
            IncidentEvidenceReader incidentRepository,
            IncidentResolutionPort incidentCommandService,
            IamContextFacade iamContextFacade,
            AssetManagementContextFacade assetManagementContextFacade,
            MonitoringContextFacade monitoringContextFacade,
            MaintenanceManagementContextFacade maintenanceManagementContextFacade,
            AiAssistanceCommandService aiAssistanceCommandService,
            ObjectMapper jsonMapper,
            SubscriptionBillingContextFacade subscriptionBillingContextFacade
    ) {
        this.aiResolutionPlanRepository = aiResolutionPlanRepository;
        this.incidentRepository = incidentRepository;
        this.incidentCommandService = incidentCommandService;
        this.iamContextFacade = iamContextFacade;
        this.assetManagementContextFacade = assetManagementContextFacade;
        this.monitoringContextFacade = monitoringContextFacade;
        this.maintenanceManagementContextFacade = maintenanceManagementContextFacade;
        this.aiAssistanceCommandService = aiAssistanceCommandService;
        this.jsonMapper = jsonMapper;
        this.subscriptionBillingContextFacade = subscriptionBillingContextFacade;
    }

    /**
     * Generates an advisory plan from backend-owned incident context and persists it as pending.
     *
     * @param command command containing organization and incident identifiers
     * @return success with persisted plan or failure with command/provider error
     */
    @Override
    public Result<AiResolutionPlan, AiResolutionPlanCommandFailure> handle(GenerateAiResolutionPlanCommand command) {
        if (!iamContextFacade.organizationExists(command.organizationId())) {
            log.warn("Organization not found for AI resolution plan generation: organizationId={}",
                    command.organizationId());
            return Result.failure(new AiResolutionPlanCommandFailure.OrganizationNotFound());
        }

        var incident = incidentRepository.findByIdAndOrganizationId(command.incidentId(), command.organizationId());
        if (incident.isEmpty()) {
            log.warn("Incident not found for AI resolution plan generation: organizationId={}, incidentId={}",
                    command.organizationId(), command.incidentId());
            return Result.failure(new AiResolutionPlanCommandFailure.IncidentNotFound());
        }
        if (!incident.get().isOpen() && !incident.get().isAcknowledged()) {
            log.warn("Incident is not active for AI resolution plan generation: organizationId={}, incidentId={}",
                    command.organizationId(), command.incidentId());
            return Result.failure(new AiResolutionPlanCommandFailure.IncidentNotActive());
        }
        var entitlement = subscriptionBillingContextFacade.checkEntitlement(
                command.organizationId(),
                ENTITLEMENT_AI_GUIDANCE
        );
        if (entitlement.isPresent() && !Boolean.TRUE.equals(entitlement.get().enabled())) {
            log.warn("AI resolution plan generation blocked by plan limit: organizationId={}, entitlement={}",
                    command.organizationId(), ENTITLEMENT_AI_GUIDANCE);
            return Result.failure(new AiResolutionPlanCommandFailure.PlanLimitExceeded(entitlement.get()));
        }

        var context = buildIncidentPlanContext(command.organizationId(), incident.get());
        var serializedContext = serializeContext(context);
        if (serializedContext.isFailure()) {
            return Result.failure(serializedContext.failure().orElseThrow());
        }

        var generatedPlan = aiAssistanceCommandService.handle(
                new GenerateIncidentResolutionPlanDraftCommand(serializedContext.success().orElseThrow())
        );
        if (generatedPlan.isFailure()) {
            return Result.failure(new AiResolutionPlanCommandFailure.ProviderFailure(
                    generatedPlan.failure().orElseThrow()
            ));
        }

        var createPlanCommand = toCreateCommand(
                command,
                generatedPlan.success().orElseThrow(),
                context
        );
        var plan = aiResolutionPlanRepository.save(new AiResolutionPlan(createPlanCommand));
        log.info("AI resolution plan generated: id={}, organizationId={}, incidentId={}",
                plan.getId(), plan.getOrganizationId(), plan.getIncidentId());
        return Result.success(plan);
    }

    /**
     * Persists generated advisory content as a pending plan.
     *
     * @param command command containing generated content
     * @return success with persisted plan or failure with command error
     */
    @Override
    @Transactional
    public Result<AiResolutionPlan, AiResolutionPlanCommandFailure> handle(CreateAiResolutionPlanCommand command) {
        var scopeFailure = validateScope(command.organizationId(), command.incidentId());
        if (scopeFailure.isPresent()) {
            return Result.failure(scopeFailure.get());
        }

        var plan = aiResolutionPlanRepository.save(new AiResolutionPlan(command));
        log.info("AI resolution plan created: id={}, organizationId={}, incidentId={}",
                plan.getId(), plan.getOrganizationId(), plan.getIncidentId());
        return Result.success(plan);
    }

    /**
     * Approves a pending plan and stores final operator-edited fields.
     *
     * @param command approval command
     * @return success with updated plan or failure with command error
     */
    @Override
    @Transactional
    public Result<AiResolutionPlan, AiResolutionPlanCommandFailure> handle(ApproveAiResolutionPlanCommand command) {
        var plan = findPendingPlan(command.organizationId(), command.incidentId(), command.planId());
        if (plan.isFailure()) {
            return plan;
        }

        var pendingPlan = plan.success().orElseThrow();
        var incidentResolution = incidentCommandService.handle(new ResolveIncidentWithCorrectiveActionCommand(
                command.organizationId(),
                command.incidentId(),
                command.approvedBy(),
                command.finalCorrectiveAction(),
                command.finalResolutionNotes()
        ));
        if (incidentResolution.isFailure()) {
            return Result.failure(incidentResolution.failure().orElseThrow());
        }

        pendingPlan.approve(command);
        var approvedPlan = aiResolutionPlanRepository.save(pendingPlan);
        log.info("AI resolution plan approved: id={}, organizationId={}, incidentId={}",
                approvedPlan.getId(), approvedPlan.getOrganizationId(), approvedPlan.getIncidentId());
        return Result.success(approvedPlan);
    }

    /**
     * Rejects a pending plan and stores audit metadata.
     *
     * @param command rejection command
     * @return success with updated plan or failure with command error
     */
    @Override
    @Transactional
    public Result<AiResolutionPlan, AiResolutionPlanCommandFailure> handle(RejectAiResolutionPlanCommand command) {
        var plan = findPendingPlan(command.organizationId(), command.incidentId(), command.planId());
        if (plan.isFailure()) {
            return plan;
        }

        var pendingPlan = plan.success().orElseThrow();
        pendingPlan.reject(command);
        var rejectedPlan = aiResolutionPlanRepository.save(pendingPlan);
        log.info("AI resolution plan rejected: id={}, organizationId={}, incidentId={}",
                rejectedPlan.getId(), rejectedPlan.getOrganizationId(), rejectedPlan.getIncidentId());
        return Result.success(rejectedPlan);
    }

    private Result<String, AiResolutionPlanCommandFailure> serializeContext(IncidentPlanContext context) {
        try {
            return Result.success(jsonMapper.writeValueAsString(context));
        } catch (JsonProcessingException exception) {
            log.warn("Incident context serialization failed for AI resolution plan", exception);
            return Result.failure(new AiResolutionPlanCommandFailure.ContextAssemblyFailed());
        }
    }

    private CreateAiResolutionPlanCommand toCreateCommand(
            GenerateAiResolutionPlanCommand command,
            AiGeneratedResponse<IncidentResolutionPlanDraft> generatedPlan,
            IncidentPlanContext context
    ) {
        var draft = generatedPlan.content();
        var escalation = draft.escalationRecommendation();
        return new CreateAiResolutionPlanCommand(
                command.organizationId(),
                command.incidentId(),
                draft.summary(),
                draft.probableCause(),
                draft.recommendedSteps().stream()
                        .map(step -> new AiResolutionPlanStep(
                                step.sequence(),
                                step.action(),
                                step.rationale(),
                                step.expectedOutcome()
                        ))
                        .toList(),
                draft.correctiveActionDraft(),
                draft.resolutionNotesDraft(),
                escalation.recommended(),
                escalation.urgency(),
                escalation.reason(),
                draft.requiredEvidence(),
                draft.uncertaintyNotes(),
                generatedPlan.modelProvider(),
                generatedPlan.modelName(),
                buildProviderMetadata(context)
        );
    }

    private String buildProviderMetadata(IncidentPlanContext context) {
        return "contextVersion=%s;recentReadings=%d;maintenanceSchedules=%d;technicalServiceRequests=%d"
                .formatted(
                        context.contextVersion(),
                        context.recentReadings().size(),
                        context.maintenance().schedules().size(),
                        context.maintenance().technicalServiceRequests().size()
                );
    }

    private IncidentPlanContext buildIncidentPlanContext(Long organizationId, IncidentEvidenceSnapshot incident) {
        var primaryReading = incident.getReadingId() == null
                ? Optional.<SensorReadingSnapshot>empty()
                : monitoringContextFacade.fetchSensorReadingByIdAndOrganizationId(
                        organizationId,
                        incident.getReadingId()
                );
        var deviceId = firstNonNull(
                incident.getDeviceId(),
                primaryReading.map(SensorReadingSnapshot::iotDeviceId).orElse(null)
        );
        var device = deviceId == null
                ? Optional.<IoTDeviceSnapshot>empty()
                : assetManagementContextFacade.fetchIoTDeviceByIdAndOrganizationId(organizationId, deviceId);
        var assetId = firstNonNull(
                incident.getAssetId(),
                primaryReading.map(SensorReadingSnapshot::assetId).orElse(null),
                device.map(IoTDeviceSnapshot::assetId).orElse(null)
        );
        var asset = assetId == null
                ? Optional.<AssetSnapshot>empty()
                : assetManagementContextFacade.fetchAssetByIdAndOrganizationId(organizationId, assetId);
        var gatewayId = firstNonNull(
                device.map(IoTDeviceSnapshot::gatewayId).orElse(null),
                primaryReading.map(SensorReadingSnapshot::gatewayId).orElse(null)
        );
        var gateway = gatewayId == null
                ? Optional.<GatewaySnapshot>empty()
                : assetManagementContextFacade.fetchGatewayByIdAndOrganizationId(organizationId, gatewayId);
        var assetSettings = assetId == null
                ? Optional.<AssetSettingsSnapshot>empty()
                : assetManagementContextFacade.fetchEffectiveAssetSettingsByAssetId(organizationId, assetId);
        var recentReadings = fetchRecentReadings(organizationId, assetId, deviceId, incident.getReadingId());
        var maintenanceSchedules = assetId == null
                ? List.<MaintenanceScheduleSnapshot>of()
                : maintenanceManagementContextFacade
                        .fetchMaintenanceSchedulesByOrganizationIdAndAssetId(organizationId, assetId)
                        .stream()
                        .limit(MAX_MAINTENANCE_RECORDS)
                        .toList();
        var technicalServiceRequests = fetchRelevantTechnicalServiceRequests(
                organizationId,
                assetId,
                incident.getId()
        );
        var evidenceNotes = buildEvidenceNotes(
                incident,
                assetId,
                primaryReading,
                asset,
                device,
                gateway,
                assetSettings,
                recentReadings,
                maintenanceSchedules,
                technicalServiceRequests
        );

        return new IncidentPlanContext(
                "incident-resolution-plan.v1",
                toIncidentContext(incident),
                asset.map(this::toAssetContext).orElse(null),
                device.map(this::toDeviceContext).orElse(null),
                gateway.map(this::toGatewayContext).orElse(null),
                assetSettings.map(this::toAssetSettingsContext).orElse(null),
                primaryReading.map(this::toReadingContext).orElse(null),
                recentReadings.stream().map(this::toReadingContext).toList(),
                new MaintenanceContext(
                        maintenanceSchedules.stream().map(this::toMaintenanceScheduleContext).toList(),
                        technicalServiceRequests.stream().map(this::toTechnicalServiceRequestContext).toList()
                ),
                evidenceNotes
        );
    }

    private List<SensorReadingSnapshot> fetchRecentReadings(
            Long organizationId,
            Long assetId,
            Long deviceId,
            Long readingId
    ) {
        if (assetId == null && deviceId == null && readingId == null) {
            return List.of();
        }
        return monitoringContextFacade.fetchSensorReadingsByOrganizationId(organizationId).stream()
                .filter(reading -> belongsToIncidentContext(reading, assetId, deviceId, readingId))
                .sorted(this::compareReadingByRecordedAtDesc)
                .limit(MAX_RECENT_READINGS)
                .toList();
    }

    private boolean belongsToIncidentContext(
            SensorReadingSnapshot reading,
            Long assetId,
            Long deviceId,
            Long readingId
    ) {
        if (assetId != null) {
            return Objects.equals(reading.assetId(), assetId);
        }
        if (deviceId != null) {
            return Objects.equals(reading.iotDeviceId(), deviceId);
        }
        return Objects.equals(reading.id(), readingId);
    }

    private int compareReadingByRecordedAtDesc(SensorReadingSnapshot left, SensorReadingSnapshot right) {
        if (left.recordedAt() == null && right.recordedAt() == null) {
            return 0;
        }
        if (left.recordedAt() == null) {
            return 1;
        }
        if (right.recordedAt() == null) {
            return -1;
        }
        return right.recordedAt().compareTo(left.recordedAt());
    }

    private List<TechnicalServiceRequestSnapshot> fetchRelevantTechnicalServiceRequests(
            Long organizationId,
            Long assetId,
            Long incidentId
    ) {
        var requestsById = new LinkedHashMap<Long, TechnicalServiceRequestSnapshot>();
        maintenanceManagementContextFacade
                .fetchTechnicalServiceRequestsByOrganizationIdAndIncidentId(organizationId, incidentId)
                .forEach(request -> requestsById.put(request.id(), request));
        if (assetId != null) {
            maintenanceManagementContextFacade
                    .fetchTechnicalServiceRequestsByOrganizationIdAndAssetId(organizationId, assetId)
                    .forEach(request -> requestsById.putIfAbsent(request.id(), request));
        }
        return requestsById.values().stream()
                .limit(MAX_MAINTENANCE_RECORDS)
                .toList();
    }

    private List<String> buildEvidenceNotes(
            IncidentEvidenceSnapshot incident,
            Long assetId,
            Optional<SensorReadingSnapshot> primaryReading,
            Optional<AssetSnapshot> asset,
            Optional<IoTDeviceSnapshot> device,
            Optional<GatewaySnapshot> gateway,
            Optional<AssetSettingsSnapshot> assetSettings,
            List<SensorReadingSnapshot> recentReadings,
            List<MaintenanceScheduleSnapshot> maintenanceSchedules,
            List<TechnicalServiceRequestSnapshot> technicalServiceRequests
    ) {
        var notes = new ArrayList<String>();
        if (incident.getReadingId() != null && primaryReading.isEmpty()) {
            notes.add("The incident references a sensor reading that could not be loaded.");
        }
        if (recentReadings.isEmpty()) {
            notes.add("No recent sensor readings were found for the incident asset or device.");
        }
        if (assetId != null && asset.isEmpty()) {
            notes.add("The incident asset could not be loaded from asset management.");
        }
        if (incident.getDeviceId() != null && device.isEmpty()) {
            notes.add("The incident IoT device could not be loaded from asset management.");
        }
        if (gateway.isEmpty()) {
            notes.add("Gateway context is unavailable for this incident.");
        }
        if (assetId != null && assetSettings.isEmpty()) {
            notes.add("Effective threshold configuration is unavailable for this asset.");
        }
        if (maintenanceSchedules.isEmpty()) {
            notes.add("No preventive maintenance schedule was found for this asset.");
        }
        if (technicalServiceRequests.isEmpty()) {
            notes.add("No related technical service request was found for this incident or asset.");
        }
        return notes;
    }

    private IncidentContext toIncidentContext(IncidentEvidenceSnapshot incident) {
        return new IncidentContext(
                incident.getId(),
                incident.getOrganizationId(),
                incident.getAssetId(),
                incident.getDeviceId(),
                incident.getReadingId(),
                limitText(incident.getAssetName(), 120),
                limitText(incident.getDeviceName(), 120),
                limitText(incident.getType(), 120),
                incident.getSeverity() == null ? null : incident.getSeverity(),
                incident.getStatus() == null ? null : incident.getStatus(),
                limitText(incident.getValue(), 180),
                incident.getDetectedAt(),
                incident.getAcknowledgedAt(),
                limitText(incident.getAcknowledgedBy(), 120),
                incident.getEscalatedAt(),
                limitText(incident.getEscalatedBy(), 120),
                limitText(incident.getEscalationReason(), 240),
                incident.getCorrectiveActionRegisteredAt(),
                limitText(incident.getCorrectiveActionRegisteredBy(), 120),
                limitText(incident.getCorrectiveAction(), 320)
        );
    }

    private AssetContext toAssetContext(AssetSnapshot asset) {
        return new AssetContext(asset.id(), asset.locationId(), limitText(asset.name(), 120));
    }

    private DeviceContext toDeviceContext(IoTDeviceSnapshot device) {
        return new DeviceContext(
                device.id(),
                device.gatewayId(),
                device.assetId(),
                limitText(device.name(), 120),
                limitText(device.status(), 80),
                copyList(device.measurementParameters())
        );
    }

    private GatewayContext toGatewayContext(GatewaySnapshot gateway) {
        return new GatewayContext(gateway.id(), gateway.locationId(), limitText(gateway.status(), 80));
    }

    private AssetSettingsContext toAssetSettingsContext(AssetSettingsSnapshot settings) {
        return new AssetSettingsContext(
                settings.minimumTemperature(),
                settings.maximumTemperature(),
                settings.minimumHumidity(),
                settings.maximumHumidity()
        );
    }

    private ReadingContext toReadingContext(SensorReadingSnapshot reading) {
        return new ReadingContext(
                reading.id(),
                reading.assetId(),
                reading.iotDeviceId(),
                reading.gatewayId(),
                reading.locationId(),
                reading.temperature(),
                reading.humidity(),
                reading.outOfRange(),
                reading.recordedAt()
        );
    }

    private MaintenanceScheduleContext toMaintenanceScheduleContext(MaintenanceScheduleSnapshot schedule) {
        return new MaintenanceScheduleContext(
                schedule.id(),
                limitText(schedule.uuid(), 120),
                schedule.assetId(),
                schedule.scheduledDate(),
                schedule.frequencyDays(),
                schedule.responsibleUserId(),
                limitText(schedule.observations(), 240),
                limitText(schedule.status(), 80),
                schedule.registeredAt()
        );
    }

    private TechnicalServiceRequestContext toTechnicalServiceRequestContext(TechnicalServiceRequestSnapshot request) {
        return new TechnicalServiceRequestContext(
                request.id(),
                limitText(request.code(), 120),
                request.assetId(),
                request.assetLocationId(),
                limitText(request.assetName(), 120),
                request.incidentId(),
                limitText(request.issueDescription(), 320),
                limitText(request.priority(), 80),
                limitText(request.status(), 80),
                limitText(request.requestedBy(), 120),
                request.requestedAt(),
                request.closedAt(),
                limitText(request.closureSummary(), 240),
                limitText(request.evidence(), 240),
                limitText(request.closedBy(), 120)
        );
    }

    private Long firstNonNull(Long first, Long second) {
        return first != null ? first : second;
    }

    private Long firstNonNull(Long first, Long second, Long third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private String limitText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private List<String> copyList(List<String> source) {
        return source == null ? List.of() : List.copyOf(source);
    }

    private Result<AiResolutionPlan, AiResolutionPlanCommandFailure> findPendingPlan(
            Long organizationId,
            Long incidentId,
            Long planId
    ) {
        var scopeFailure = validateScope(organizationId, incidentId);
        if (scopeFailure.isPresent()) {
            return Result.failure(scopeFailure.get());
        }

        var plan = aiResolutionPlanRepository.findByIdAndIncidentIdAndOrganizationId(
                planId,
                incidentId,
                organizationId
        );
        if (plan.isEmpty()) {
            log.warn("AI resolution plan not found: organizationId={}, incidentId={}, planId={}",
                    organizationId, incidentId, planId);
            return Result.failure(new AiResolutionPlanCommandFailure.PlanNotFound());
        }
        if (!plan.get().isPending()) {
            log.warn("AI resolution plan already decided: organizationId={}, incidentId={}, planId={}",
                    organizationId, incidentId, planId);
            return Result.failure(new AiResolutionPlanCommandFailure.PlanAlreadyDecided());
        }
        return Result.success(plan.get());
    }

    private Optional<AiResolutionPlanCommandFailure> validateScope(Long organizationId, Long incidentId) {
        if (!iamContextFacade.organizationExists(organizationId)) {
            log.warn("Organization not found for AI resolution plan command: organizationId={}", organizationId);
            return Optional.of(new AiResolutionPlanCommandFailure.OrganizationNotFound());
        }
        if (!incidentRepository.existsByIdAndOrganizationId(incidentId, organizationId)) {
            log.warn("Incident not found for AI resolution plan command: organizationId={}, incidentId={}",
                    organizationId, incidentId);
            return Optional.of(new AiResolutionPlanCommandFailure.IncidentNotFound());
        }
        return Optional.empty();
    }

    private record IncidentPlanContext(
            String contextVersion,
            IncidentContext incident,
            AssetContext asset,
            DeviceContext device,
            GatewayContext gateway,
            AssetSettingsContext thresholdConfiguration,
            ReadingContext primaryReading,
            List<ReadingContext> recentReadings,
            MaintenanceContext maintenance,
            List<String> evidenceNotes
    ) {
    }

    private record IncidentContext(
            Long id,
            Long organizationId,
            Long assetId,
            Long deviceId,
            Long readingId,
            String assetName,
            String deviceName,
            String type,
            String severity,
            String status,
            String value,
            Instant detectedAt,
            Instant acknowledgedAt,
            String acknowledgedBy,
            Instant escalatedAt,
            String escalatedBy,
            String escalationReason,
            Instant correctiveActionRegisteredAt,
            String correctiveActionRegisteredBy,
            String correctiveAction
    ) {
    }

    private record AssetContext(Long id, Long locationId, String name) {
    }

    private record DeviceContext(
            Long id,
            Long gatewayId,
            Long assetId,
            String name,
            String status,
            List<String> measurementParameters
    ) {
    }

    private record GatewayContext(Long id, Long locationId, String status) {
    }

    private record AssetSettingsContext(
            Double minimumTemperature,
            Double maximumTemperature,
            Double minimumHumidity,
            Double maximumHumidity
    ) {
    }

    private record ReadingContext(
            Long id,
            Long assetId,
            Long iotDeviceId,
            Long gatewayId,
            Long locationId,
            Double temperature,
            Double humidity,
            Boolean outOfRange,
            OffsetDateTime recordedAt
    ) {
    }

    private record MaintenanceContext(
            List<MaintenanceScheduleContext> schedules,
            List<TechnicalServiceRequestContext> technicalServiceRequests
    ) {
    }

    private record MaintenanceScheduleContext(
            Long id,
            String uuid,
            Long assetId,
            OffsetDateTime scheduledDate,
            Integer frequencyDays,
            Long responsibleUserId,
            String observations,
            String status,
            OffsetDateTime registeredAt
    ) {
    }

    private record TechnicalServiceRequestContext(
            Long id,
            String code,
            Long assetId,
            Long assetLocationId,
            String assetName,
            Long incidentId,
            String issueDescription,
            String priority,
            String status,
            String requestedBy,
            OffsetDateTime requestedAt,
            OffsetDateTime closedAt,
            String closureSummary,
            String evidence,
            String closedBy
    ) {
    }
}
