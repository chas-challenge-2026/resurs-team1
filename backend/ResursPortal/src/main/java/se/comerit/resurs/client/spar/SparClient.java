package se.comerit.resurs.client.spar;

import se.comerit.resurs.dto.spar.SparPersonDTO;

import java.util.Optional;

public interface SparClient {
    Optional<SparPersonDTO> lookup(String personalNumber);
}
