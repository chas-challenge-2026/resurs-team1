package se.comerit.resurs.dto.application;

import java.time.Instant;

public record ApplicationSignatureDTO(String signerName, Instant signedAt) {
}
