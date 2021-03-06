package uk.nhs.adaptors.gp2gp.gpc;

import static uk.nhs.adaptors.gp2gp.gpc.GpcFileNameConstants.GPC_STRUCTURED_FILE_EXTENSION;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.storage.StorageConnectorService;
import uk.nhs.adaptors.gp2gp.common.task.TaskExecutor;
import uk.nhs.adaptors.gp2gp.ehr.EhrExtractStatusService;
import uk.nhs.adaptors.gp2gp.ehr.mapper.EhrExtractMapper;
import uk.nhs.adaptors.gp2gp.ehr.mapper.MessageContext;
import uk.nhs.adaptors.gp2gp.ehr.mapper.OutputMessageWrapperMapper;
import uk.nhs.adaptors.gp2gp.ehr.model.EhrExtractStatus;
import uk.nhs.adaptors.gp2gp.gpc.builder.GpcRequestBuilder;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class GetGpcStructuredTaskExecutor implements TaskExecutor<GetGpcStructuredTaskDefinition> {
    private final GpcClient gpcClient;
    private final GpcRequestBuilder gpcRequestBuilder;
    private final StorageConnectorService storageConnectorService;
    private final EhrExtractStatusService ehrExtractStatusService;
    private final DetectTranslationCompleteService detectTranslationCompleteService;
    private final OutputMessageWrapperMapper outputMessageWrapperMapper;
    private final EhrExtractMapper ehrExtractMapper;
    private final MessageContext messageContext;
    private final FhirParseService fhirParseService;

    @Override
    public Class<GetGpcStructuredTaskDefinition> getTaskType() {
        return GetGpcStructuredTaskDefinition.class;
    }

    @Override
    public void execute(GetGpcStructuredTaskDefinition structuredTaskDefinition) {
        LOGGER.info("Execute called from GetGpcStructuredTaskExecutor");

        var response = gpcClient.getStructuredRecord(structuredTaskDefinition);
        var hl7TranslatedResponse = StringUtils.EMPTY;

        try {
            Bundle bundle = fhirParseService.parseResource(response, Bundle.class);
            messageContext.initialize(bundle);

            var ehrExtractTemplateParameters = ehrExtractMapper.mapBundleToEhrFhirExtractParams(
                structuredTaskDefinition,
                bundle);
            String ehrExtractContent = ehrExtractMapper.mapEhrExtractToXml(ehrExtractTemplateParameters);

            hl7TranslatedResponse = outputMessageWrapperMapper.map(
                structuredTaskDefinition,
                ehrExtractContent);
        } finally {
            messageContext.resetMessageContext();
        }

        String fileName = structuredTaskDefinition.getConversationId() + GPC_STRUCTURED_FILE_EXTENSION;
        storageConnectorService.uploadFile(StorageDataWrapperProvider.buildStorageDataWrapper(structuredTaskDefinition,
            hl7TranslatedResponse,
            structuredTaskDefinition.getTaskId()),
            fileName);

        EhrExtractStatus ehrExtractStatus = ehrExtractStatusService.updateEhrExtractStatusAccessStructured(structuredTaskDefinition);

        detectTranslationCompleteService.beginSendingCompleteExtract(ehrExtractStatus);
    }
}
