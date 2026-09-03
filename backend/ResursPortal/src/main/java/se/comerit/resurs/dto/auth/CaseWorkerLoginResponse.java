package se.comerit.resurs.dto.auth;

public record CaseWorkerLoginResponse(Long userId,
                                      String role,
                                      String name,
                                      String email) {
}
