package uk.nhs.adaptors.gp2gp.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MhsInboundMessage {
    private final String ebXML;
    private final String payload;
    private final List<String> attachments;
}
