package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils.extractTextOrCoding;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.utils.CodeableConceptMappingUtils;
import uk.nhs.adaptors.gp2gp.ehr.utils.DateFormatUtil;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DiaryPlanStatementMapper {

    private static final Mustache PLAN_STATEMENT_TEMPLATE = TemplateUtils.loadTemplate("ehr_plan_statement_template.mustache");
    private static final String EMPTY_DATE = "nullFlavor=\"UNK\"";
    private static final String FULL_DATE = "value=\"%s\"";
    public static final String REASON_CODE_TEXT_FORMAT = "Reason Code: %s";
    public static final String EARLIEST_RECALL_DATE_FORMAT = "Earliest Recall Date: %s";
    public static final String RECALL_DEVICE = "Recall Device: %s %s";
    public static final String RECALL_ORGANISATION = "Recall Organisation: %s";

    private final MessageContext messageContext;
    private final CodeableConceptCdMapper codeableConceptCdMapper;

    public String mapDiaryProcedureRequestToPlanStatement(ProcedureRequest procedureRequest, Boolean isNested) {
        if (procedureRequest.getIntent() != ProcedureRequest.ProcedureRequestIntent.PLAN) {
            return StringUtils.EMPTY;
        }

        PlanStatementMapperParameters.PlanStatementMapperParametersBuilder builder = PlanStatementMapperParameters.builder()
            .isNested(isNested)
            .id(messageContext.getIdMapper().getOrNew(ResourceType.ProcedureRequest, procedureRequest.getId()))
            .availabilityTime(buildAvailabilityTime(procedureRequest));

        buildEffectiveTime(procedureRequest).map(builder::effectiveTime);
        buildText(procedureRequest).map(builder::text);
        builder.code(buildCode(procedureRequest));

        return TemplateUtils.fillTemplate(PLAN_STATEMENT_TEMPLATE, builder.build());
    }

    private Optional<String> buildEffectiveTime(ProcedureRequest procedureRequest) {
        DateTimeType date = null;
        if (procedureRequest.hasOccurrenceDateTimeType()) {
            date = procedureRequest.getOccurrenceDateTimeType();
        } else if (procedureRequest.hasOccurrencePeriod()) {
            Period occurrencePeriod = procedureRequest.getOccurrencePeriod();
            date = occurrencePeriod.hasEnd() ? occurrencePeriod.getEndElement() : occurrencePeriod.getStartElement();
        }

        return Optional.of(formatEffectiveDate(date));
    }

    private String formatEffectiveDate(DateTimeType date) {
        return date != null ? String.format(FULL_DATE, DateFormatUtil.toHl7Format(date)) : EMPTY_DATE;
    }

    private String buildAvailabilityTime(ProcedureRequest procedureRequest) {
        if (procedureRequest.hasAuthoredOn()) {
            return DateFormatUtil.toHl7Format(procedureRequest.getAuthoredOnElement());
        }

        throw new EhrMapperException(
            String.format("ProcedureRequest: %s does not have required authoredOn", procedureRequest.getId())
        );
    }

    private Optional<String> buildText(ProcedureRequest procedureRequest) {
        return Optional.of(Stream.of(
            getEarliestRecallDate(procedureRequest),
            getRequester(procedureRequest),
            getReasonCode(procedureRequest),
            getNotes(procedureRequest)
        )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining(StringUtils.SPACE)));
    }

    private Optional<String> getEarliestRecallDate(ProcedureRequest procedureRequest) {
        if (procedureRequest.hasOccurrencePeriod()
            && procedureRequest.getOccurrencePeriod().hasStart()) {
            return Optional.of(formatStartDate(procedureRequest));
        }

        return Optional.empty();
    }

    private String formatStartDate(ProcedureRequest procedureRequest) {
        return String.format(EARLIEST_RECALL_DATE_FORMAT,
            DateFormatUtil.toTextFormat(procedureRequest.getOccurrencePeriod().getStartElement()));
    }

    private Optional<String> getNotes(ProcedureRequest procedureRequest) {
        return Optional.of(procedureRequest.getNote()
            .stream()
            .map(Annotation::getText)
            .collect(Collectors.joining(StringUtils.SPACE)));
    }

    private Optional<String> getReasonCode(ProcedureRequest procedureRequest) {
        return procedureRequest.getReasonCode()
            .stream()
            .map(CodeableConceptMappingUtils::extractTextOrCoding)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(this::formatReason)
            .findFirst();
    }

    private String formatReason(String value) {
        return String.format(REASON_CODE_TEXT_FORMAT, value);
    }

    private Optional<String> getRequester(ProcedureRequest procedureRequest) {
        Reference agent = procedureRequest.getRequester().getAgent();

        if (agent.hasReference()) {
            IIdType reference = agent.getReferenceElement();
            if (reference.getResourceType().equals(ResourceType.Organization.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(resource -> (Organization) resource)
                    .map(this::formatOrganization);
            } else if (reference.getResourceType().equals(ResourceType.Device.name())) {
                return messageContext.getInputBundleHolder()
                    .getResource(reference)
                    .map(resource -> (Device) resource)
                    .map(this::formatDevice);
            }
        }

        return Optional.empty();
    }

    private String buildCode(ProcedureRequest procedureRequest) {
        if (procedureRequest.hasCode()) {
            return codeableConceptCdMapper.mapCodeableConceptToCd(procedureRequest.getCode());
        }
        throw new EhrMapperException("Procedure request code not present");
    }

    private String formatDevice(Device device) {
        return String.format(RECALL_DEVICE, extractTextOrCoding(device.getType()).orElse(StringUtils.EMPTY), device.getManufacturer());
    }

    private String formatOrganization(Organization organization) {
        return String.format(RECALL_ORGANISATION, organization.getName());
    }

    @Getter
    @Setter
    @Builder
    public static class PlanStatementMapperParameters {
        private Boolean isNested;
        private String id;
        private String text;
        private String availabilityTime;
        private String effectiveTime;
        private String code;
    }
}
