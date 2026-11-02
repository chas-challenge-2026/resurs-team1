package se.comerit.resurs.client.spar;

import se.comerit.resurs.dto.spar.SparPersonDTO;

import java.util.Map;

final class MockSparData {

    static final String UNAVAILABLE_PERSONAL_NUMBER = "550101-5555";

    static final Map<String, SparPersonDTO> REGISTRY = Map.of(
            "750312-1234", new SparPersonDTO("750312-1234", false),
            "820624-5678", new SparPersonDTO("820624-5678", false),
            "660930-7777", new SparPersonDTO("660930-7777", false),
            "710214-7777", new SparPersonDTO("710214-7777", true),
            "681105-9999", new SparPersonDTO("681105-9999", false)
    );

    private MockSparData() {
    }
}