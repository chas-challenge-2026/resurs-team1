package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.enums.AuditAction;
import se.comerit.resurs.persistence.AuditEventRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.model.AuditEvent;
import se.comerit.resurs.persistence.model.CreditApplication;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final CreditApplicationRepository applicationRepository;


    public AuditService (AuditEventRepository auditEventRepository, CreditApplicationRepository applicationRepository){
        this.auditEventRepository = auditEventRepository;
        this.applicationRepository = applicationRepository;
    }
    // TODO: previousHash, signingKeyId och signature fylls av signeringsmodulen
    //       (JNA, se v2-targets.md punkt 4). null tills den är på plats.
    private static final String NOT_SIGNED_YET = null;

    public void applicationCreated(CreditApplication application){
            AuditEvent event = new AuditEvent(application,nextSequenceNumber(application), AuditAction.APPLICATION_CREATED, application.getCompany().getOrg_number(),"{\"actorType\":\"COMPANY\",\"purpose\":\"" + application.getPurpose() + "\"}", NOT_SIGNED_YET, NOT_SIGNED_YET, NOT_SIGNED_YET);
            auditEventRepository.save(event);
    }

    public void scoringRun(CreditApplication application, int flags){
        AuditEvent event = new AuditEvent(application, nextSequenceNumber(application),AuditAction.SCORING_RUN,
                "SYSTEM",
                "{\"actorType\":\"SYSTEM\""
                    + ",\"decision\":\"" + application.getDecision() + "\""
                    + ",\"newStatus\":\"" + application.getStatus() + "\""
                    + ",\"flags\":" + flags + "}",
                NOT_SIGNED_YET, NOT_SIGNED_YET, NOT_SIGNED_YET
        );
        auditEventRepository.save(event);
    }

    public void documentUploaded(CreditApplication application, String fileName, String docType){
        AuditEvent event = new AuditEvent(application, nextSequenceNumber(application), AuditAction.DOCUMENT_UPLOADED,
                application.getCompany().getOrg_number(), "{\"actorType\":\"COMPANY\",\"filename\":\"" + fileName + "\",\"docType\":\"" + docType + "\"}", NOT_SIGNED_YET, NOT_SIGNED_YET, NOT_SIGNED_YET);
        auditEventRepository.save(event);
    }

    public void manualDecision(CreditApplication application,String workerEmail, String workerName,
                               ApplicationStatus previousStatus, String comment){
        AuditEvent event = new AuditEvent(application, nextSequenceNumber(application), AuditAction.MANUAL_DECISION, workerEmail, "{\"actorType\":\"CASE_WORKER\""
                + ",\"workerName\":\"" + workerName + "\""
                + ",\"previousStatus\":\"" + previousStatus + "\""
                + ",\"newStatus\":\"" + application.getStatus() + "\""
                + (comment == null || comment.isBlank() ? "" : ",\"comment\":\"" + comment + "\"")
                + "}",  NOT_SIGNED_YET, NOT_SIGNED_YET, NOT_SIGNED_YET);
        auditEventRepository.save(event);
    }

    private long nextSequenceNumber(CreditApplication application){
        return auditEventRepository.findMaxSequenceNumber(application.getId()) + 1;
    }




}
