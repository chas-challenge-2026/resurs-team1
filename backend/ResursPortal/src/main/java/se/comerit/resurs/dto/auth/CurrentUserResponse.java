package se.comerit.resurs.dto.auth;

public record CurrentUserResponse(Long userId,
                                  String role,
                                  String displayName,
                                  String orgNumber) {
}
