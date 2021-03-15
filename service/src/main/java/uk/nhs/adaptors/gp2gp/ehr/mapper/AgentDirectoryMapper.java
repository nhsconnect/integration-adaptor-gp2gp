package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mustachejava.Mustache;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.ehr.mapper.parameters.AgentDirectoryParameter;
import uk.nhs.adaptors.gp2gp.ehr.utils.ResourceExtractor;
import uk.nhs.adaptors.gp2gp.ehr.utils.TemplateUtils;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class AgentDirectoryMapper {
    private static final Mustache AGENT_DIRECTORY_STRUCTURE_TEMPLATE = TemplateUtils.loadTemplate("ehr_agent_structure_template.mustache");
    private static final Map<String, Boolean> MAPPED_ORGANIZATIONS_AND_PRACTITIONER = new HashMap<>();

    private final PractitionerAgentPersonMapper practitionerAgentPersonMapper;
    private final OrganizationToAgentMapper organizationToAgentMapper;

    public String mapEHRFolderToAgentDirectory(Bundle bundle, String nhsNumber) {
        var builder = AgentDirectoryParameter.builder()
            .patientManagingOrganization(extractPatientManagingOrganization(bundle, nhsNumber))
            .observationOrganizations(prepareObservationAgentsList(bundle))
            .referralRequestsOrganizations(prepareReferralRequestAgentsList(bundle));

        var testing = preparePractitionerRoleAgentsList(bundle);

        return TemplateUtils.fillTemplate(AGENT_DIRECTORY_STRUCTURE_TEMPLATE, builder.build());
    }

    private String extractPatientManagingOrganization(Bundle bundle, String nhsNumber) {
        Optional<Patient> patientOptional = AgentDirectoryExtractor.extractPatientByNhsNumber(bundle, nhsNumber);
        if (patientOptional.isPresent() && patientOptional.get().hasManagingOrganization()) {
            var organization = ResourceExtractor.extractResourceByReference(bundle,
                patientOptional.get().getManagingOrganization().getReferenceElement());
            if (organization.isPresent()) {
                return organizationToAgentMapper.mapOrganizationToAgent((Organization) organization.get());
            }
        }
        throw new EhrMapperException("No patient or managing organization found in ehrFolder with NHS Number: " + nhsNumber);
    }

    private List<String> prepareObservationAgentsList(Bundle bundle) {
        List<Observation> observations = AgentDirectoryExtractor.extractObservationsWithPerformers(bundle);

        return observations.stream()
            .map(observation -> buildAgentPerson(observation, bundle))
            .collect(Collectors.toList());
    }

    private List<String> prepareReferralRequestAgentsList(Bundle bundle) {
        List<ReferralRequest> referralRequests = AgentDirectoryExtractor.extractReferralRequestsWithRequester(bundle);

        return referralRequests.stream()
            .map(referralRequest -> buildAgentPerson(referralRequest, bundle))
            .collect(Collectors.toList());
    }

    private List<String> preparePractitionerRoleAgentsList(Bundle bundle) {
        List<Practitioner> practitioners = AgentDirectoryExtractor.extractRemainingPractitioners(bundle);
        var testing = AgentDirectoryExtractor.extractPractitionerRoleTriples(bundle, practitioners);

        return List.of();
    }

    private String buildAgentPerson(Observation observation, Bundle bundle) {
        var practitioner = AgentDirectoryExtractor.extractObservationResource(observation, bundle, ResourceType.Practitioner);
        var organization = AgentDirectoryExtractor.extractObservationResource(observation, bundle, ResourceType.Observation);
        if (practitioner.isPresent() && organization.isPresent()) {
            return mapAgentPerson((Practitioner) practitioner.get(),
                Optional.empty(),
                organization.map(value -> (Organization) value));
        }
        return StringUtils.EMPTY;
    }

    private String buildAgentPerson(ReferralRequest referralRequest, Bundle bundle) {
        var practitioner = ResourceExtractor.extractResourceByReference(bundle, referralRequest.getRequester().getAgent().getReferenceElement());
        var organization = ResourceExtractor.extractResourceByReference(bundle, referralRequest.getRequester().getOnBehalfOf().getReferenceElement());
        if (practitioner.isPresent() && organization.isPresent()) {
            return mapAgentPerson((Practitioner) practitioner.get(),
                Optional.empty(),
                organization.map(value -> (Organization) value));
        }
        return StringUtils.EMPTY;
    }

    private String mapAgentPerson(Practitioner practitioner, Optional<PractitionerRole> practitionerRole, Optional<Organization> organization) {
        if (organizationPractitionerHasBeenMapped(practitioner, organization.get())) {
            addOrganizationPractitionerPairToMap(practitioner, organization.get());
            return practitionerAgentPersonMapper.mapPractitionerToAgentPerson(
                practitioner,
                practitionerRole,
                organization
            );
        }
        return StringUtils.EMPTY;
    }

    private boolean organizationPractitionerHasBeenMapped(Practitioner practitioner, Organization organization) {
        var joinedId = practitioner.getId() + "-" + organization.getId();
        return MAPPED_ORGANIZATIONS_AND_PRACTITIONER.containsKey(joinedId);
    }

    private void addOrganizationPractitionerPairToMap(Practitioner practitioner, Organization organization) {
        var joinedId = practitioner.getId() + "-" + organization.getId();
        MAPPED_ORGANIZATIONS_AND_PRACTITIONER.put(joinedId, true);
    }
}