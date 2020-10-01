package uk.nhs.adaptors.gp2gp.configurations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Components {
    @Bean
    public IParser fhirJsonParser() {
        return FhirContext.forDstu3().newJsonParser();
    }
}
