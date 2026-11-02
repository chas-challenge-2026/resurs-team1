package se.comerit.resurs.dto.auth;

import java.util.List;
import java.util.Set;

public record CurrentUserResponse(Long userId,
                                  Set<String> roles,
                                  String displayName,
                                  String orgNumber) {
}
