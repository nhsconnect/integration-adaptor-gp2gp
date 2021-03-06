package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Encounter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class EncounterMapperTest {
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/encounter/";
    private static final String TEST_ID = "test-id";
    private static final String INPUT_JSON_WITH_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "example-encounter-resource-1.json";
    private static final String OUTPUT_XML_WITH_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "expected-output-encounter-1.xml";
    private static final String INPUT_JSON_WITH_START_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "example-encounter-resource-2.json";
    private static final String OUTPUT_XML_WITH_START_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "expected-output-encounter-2.xml";
    private static final String INPUT_JSON_WITH_NO_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "example-encounter-resource-3.json";
    private static final String OUTPUT_XML_WITH_NO_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "expected-output-encounter-3.xml";
    private static final String INPUT_JSON_WITH_NO_PERIOD_FIELD = TEST_FILES_DIRECTORY
        + "example-encounter-resource-4.json";
    private static final String OUTPUT_XML_WITH_NO_PERIOD_FIELD = TEST_FILES_DIRECTORY
        + "expected-output-encounter-4.xml";
    private static final String INPUT_JSON_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-5.json";
    private static final String OUTPUT_XML_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-5.xml";
    private static final String INPUT_JSON_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_NO_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-6.json";
    private static final String OUTPUT_XML_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_NO_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-6.xml";
    private static final String INPUT_JSON_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_AND_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-7.json";
    private static final String OUTPUT_XML_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_AND_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-7.xml";
    private static final String INPUT_JSON_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_NO_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-8.json";
    private static final String OUTPUT_XML_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_NO_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-8.xml";
    private static final String INPUT_JSON_WITH_TYPE_NOT_SNOMED_AND_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-9.json";
    private static final String OUTPUT_XML_WITH_TYPE_NOT_SNOMED_AND_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-9.xml";
    private static final String INPUT_JSON_WITH_TYPE_NOT_SNOMED_AND_NO_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-10.json";
    private static final String OUTPUT_XML_WITH_TYPE_NOT_SNOMED_AND_NO_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-10.xml";
    private static final String INPUT_JSON_WITH_TYPE_AND_NO_CODING_AND_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-11.json";
    private static final String OUTPUT_XML_WITH_TYPE_AND_NO_CODING_AND_TEXT = TEST_FILES_DIRECTORY
        + "expected-output-encounter-11.xml";
    private static final String INPUT_JSON_WITH_TYPE_AND_NO_CODING_AND_TEXT_AND_NO_TEXT = TEST_FILES_DIRECTORY
        + "example-encounter-resource-12.json";
    private static final String OUTPUT_XML_WITH_TYPE_AND_NO_CODING_AND_TEXT_AND_NO_TEXT  = TEST_FILES_DIRECTORY
        + "expected-output-encounter-12.xml";
    private static final String INPUT_JSON_WITH_NO_TYPE = TEST_FILES_DIRECTORY
        + "example-encounter-resource-13.json";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;
    @Mock
    private EncounterComponentsMapper encounterComponentsMapper;

    private EncounterMapper encounterMapper;
    private MessageContext messageContext;

    @BeforeEach
    public void setUp() {
        messageContext = new MessageContext(randomIdGeneratorService);
        encounterMapper = new EncounterMapper(messageContext, encounterComponentsMapper);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("testFilePaths")
    public void When_MappingParsedEncounterJson_Expect_EhrCompositionXmlOutput(String input, String output) throws IOException {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        String expectedOutputMessage = ResourceTestFileUtils.getFileContent(output);

        var jsonInput = ResourceTestFileUtils.getFileContent(input);
        Encounter parsedEncounter = new FhirParseService().parseResource(jsonInput, Encounter.class);

        String outputMessage = encounterMapper.mapEncounterToEhrComposition(parsedEncounter);
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);

        verify(encounterComponentsMapper).mapComponents(parsedEncounter);
    }

    private static Stream<Arguments> testFilePaths() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_TIME, OUTPUT_XML_WITH_EFFECTIVE_TIME),
            Arguments.of(INPUT_JSON_WITH_START_EFFECTIVE_TIME, OUTPUT_XML_WITH_START_EFFECTIVE_TIME),
            Arguments.of(INPUT_JSON_WITH_NO_EFFECTIVE_TIME, OUTPUT_XML_WITH_NO_EFFECTIVE_TIME),
            Arguments.of(INPUT_JSON_WITH_NO_PERIOD_FIELD, OUTPUT_XML_WITH_NO_PERIOD_FIELD),
            Arguments.of(INPUT_JSON_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_TEXT, OUTPUT_XML_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_NO_TEXT, OUTPUT_XML_WITH_TYPE_SNOMED_AND_IN_VOCAB_AND_NO_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_AND_TEXT, OUTPUT_XML_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_AND_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_NO_TEXT, OUTPUT_XML_WITH_TYPE_SNOMED_AND_NOT_IN_VOCAB_NO_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_NOT_SNOMED_AND_TEXT, OUTPUT_XML_WITH_TYPE_NOT_SNOMED_AND_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_NOT_SNOMED_AND_NO_TEXT, OUTPUT_XML_WITH_TYPE_NOT_SNOMED_AND_NO_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_AND_NO_CODING_AND_TEXT, OUTPUT_XML_WITH_TYPE_AND_NO_CODING_AND_TEXT),
            Arguments.of(INPUT_JSON_WITH_TYPE_AND_NO_CODING_AND_TEXT_AND_NO_TEXT, OUTPUT_XML_WITH_TYPE_AND_NO_CODING_AND_TEXT_AND_NO_TEXT)
        );
    }

    @Test
    public void When_MappingEncounterWithNoType_Expect_Exception() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NO_TYPE);

        Encounter parsedEncounter = new FhirParseService().parseResource(jsonInput, Encounter.class);

        assertThrows(EhrMapperException.class, ()
            -> encounterMapper.mapEncounterToEhrComposition(parsedEncounter));
    }
}
