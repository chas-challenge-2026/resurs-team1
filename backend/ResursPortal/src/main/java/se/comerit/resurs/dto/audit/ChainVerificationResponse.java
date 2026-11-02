package se.comerit.resurs.dto.audit;

public record ChainVerificationResponse (int code, String response, Long failedIndex){
}
