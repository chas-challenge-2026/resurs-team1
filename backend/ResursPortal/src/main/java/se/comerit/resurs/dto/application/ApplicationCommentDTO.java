package se.comerit.resurs.dto.application;

import java.time.Instant;

/**
 * ApplicationCommentDTO -> en kommentar på ett ärende
 *
 * Innehåller vem som skrev kommentaren (author), texten (text) och när den skrevs (createdAt).
 * Visar inte resten av audit-händelsen t.ex e-post, hashar eller signatur.
 *
 * Representerar bara data som skickas ut -> byggs av AuditService
 *
 */

public record ApplicationCommentDTO(String author, String text, Instant createdAt) {
}
