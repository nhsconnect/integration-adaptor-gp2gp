package uk.nhs.adaptors.gp2gp.ehr.mapper;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.instance.model.api.IBaseElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class StructuredObservationValueMapper {
    private static final Map<Class<? extends IBaseElement>, Function<IBaseElement, String>> VALUE_MAPPING_FUNCTIONS =
        ImmutableMap.of(Quantity.class, value -> ObservationValueQuantityMapper.processQuantity((Quantity) value),
            StringType.class, value -> processStringType((StringType) value));
    private static final String STRING_VALUE_TEMPLATE = "<value xsi:type=\"ST\">%s</value>";
    private static final String REFERENCE_RANGE_TEMPLATE = "<referenceRange typeCode=\"REFV\">"
        + "<referenceInterpretationRange classCode=\"OBS\" moodCode=\"EVN.CRT\">"
        + "<text>%s</text>"
        + "<value>%s</value>"
        + "</referenceInterpretationRange>"
        + "</referenceRange>";
    private static final String LOW_RANGE_TEMPLATE = "<low value=\"%s\"/>";
    private static final String HIGH_RANGE_TEMPLATE = "<high value=\"%s\"/>";

    public String mapObservationValueToStructuredElement(IBaseElement value) {
        if (!isStructuredValueType(value)) {
            throw new IllegalArgumentException(
                String.format("Observation value of '%s' type can not be converted to xml element", value.getClass()));
        }
        return VALUE_MAPPING_FUNCTIONS.get(value.getClass())
            .apply(value);
    }

    public String mapReferenceRangeType(Observation.ObservationReferenceRangeComponent referenceRange) {
        if (referenceRange.hasText()) {
            String rangeValue = StringUtils.EMPTY;

            if (referenceRange.hasLow() && referenceRange.getLow().hasValue()) {
                rangeValue += String.format(LOW_RANGE_TEMPLATE, referenceRange.getLow().getValue());

            }
            if (referenceRange.hasHigh() && referenceRange.getHigh().hasValue()) {
                rangeValue += String.format(HIGH_RANGE_TEMPLATE, referenceRange.getHigh().getValue());
            }
            return String.format(REFERENCE_RANGE_TEMPLATE, referenceRange.getText(), rangeValue);
        }
        return StringUtils.EMPTY;
    }

    public boolean isStructuredValueType(IBaseElement value) {
        return VALUE_MAPPING_FUNCTIONS.containsKey(value.getClass());
    }

    private static String processStringType(StringType value) {
        if (value.hasValue()) {
            return String.format(STRING_VALUE_TEMPLATE, value.getValue());
        }
        return StringUtils.EMPTY;
    }
}
