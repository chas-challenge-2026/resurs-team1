package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.client.spar.SparClient;
import se.comerit.resurs.dto.spar.SparPersonDTO;

@Service
public class SparService {

    private final SparClient sparClient;

    public SparService(SparClient sparClient) {
        this.sparClient = sparClient;
    }

    public boolean hasProtectedIdentity(String personalNumber) {
        return sparClient.lookup(personalNumber)
                .map(SparPersonDTO::protectedIdentity)
                .orElse(true);
    }
}
