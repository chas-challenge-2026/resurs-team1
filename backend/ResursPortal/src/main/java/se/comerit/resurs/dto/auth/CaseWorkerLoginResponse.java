package se.comerit.resurs.dto.auth;

import java.util.List;
import java.util.Set;

public record CaseWorkerLoginResponse(Long userId,
                                      Set<String> roles,
                                      String name,
                                      String email) {
}
