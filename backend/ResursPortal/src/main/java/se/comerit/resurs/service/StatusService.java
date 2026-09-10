package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.DocumentDTO;
import se.comerit.resurs.dto.status.ApplicationStatusDetails;
import se.comerit.resurs.dto.status.StatusDetails;
import se.comerit.resurs.dto.status.Step;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.DocumentRepository;
import se.comerit.resurs.persistence.model.CreditApplication;

import java.util.ArrayList;
import java.util.List;


//Might want to fully rewrite this once/if we get to that point
@Service
public class StatusService {

    private final CreditApplicationRepository applicationRepository;
    private final DocumentRepository documentRepo;

    public StatusService(CreditApplicationRepository applicationRepository, DocumentRepository documentRepo) {
        this.applicationRepository = applicationRepository;
        this.documentRepo = documentRepo;
    }


    public StatusDetails showStatus(Long applicationID){

        CreditApplication application = applicationRepository.findById(applicationID).orElseThrow();

        // Previous redirect to /applications was located here

        // Hårdkodade ETA-steg — oavsett vilket steg ansökan faktiskt är på
        // TODO: beräkna dynamiskt baserat på skapelsedatum och SLA
        List<Step> steps = new ArrayList<>();

        //Steg 1
        steps.add(new Step(
                "Ansökan inlämnad",
                "—",
                StepStatus.DONE.toString(),
                "Ansökan har mottagits av systemet."));


        //Step 2 DokumentGranskning
        StepStatus stepTwoStatus;
        if (application.getStatus().equals(ApplicationStatus.PENDING_DOCS)) {
            stepTwoStatus = StepStatus.PENDING;
        } else {
            stepTwoStatus = StepStatus.DONE;
        }
        // Hårdkodat ETA — alltid "2 dagar" oavsett faktiskt läge
        steps.add(new Step(
                "Dokumentgranskning",
                "2 dagar",
                stepTwoStatus.toString(),
                "Årsredovisning och F-skatteintyg granskas."));

        //Step 3 Kreditbedömning
        StepStatus stepThreeStatus;
        if (application.getStatus().equals(ApplicationStatus.UNDER_REVIEW)) {
            stepThreeStatus = StepStatus.CURRENT;
        } else if (application.getStatus().equals(ApplicationStatus.PENDING_DOCS)) {
            stepThreeStatus = StepStatus.PENDING;
        } else {
            stepThreeStatus = StepStatus.DONE;
        }
        steps.add(new Step(
                "Kreditbedömning",
                "3 dagar",
                stepThreeStatus.toString(),
                "Finansiella nyckeltal analyseras och scoring körs."
        ) );

        StepStatus stepFourStatus;
        if (application.getStatus().equals(ApplicationStatus.APPROVED) || application.getStatus().equals(ApplicationStatus.REJECTED)) {
            stepFourStatus = StepStatus.DONE;
        } else {
            stepFourStatus = StepStatus.PENDING;
        }
        steps.add(new Step(
                "Beslut",
                "1 dag",
                stepFourStatus.toString(),
                "Kreditbeslut fattas av handläggare eller automatiskt."
        ));


        List<DocumentDTO> documents = documentRepo.findByApplicationId(applicationID).stream().map(DocumentDTO::new).toList();


        return new StatusDetails(
                new ApplicationStatusDetails(application),
                steps,
                application.getStatus(),
                documents,
                application.getAuditLog()
        );

    }


    // Total ETA-kalkyl — summerar hårdkodade värden, ger alltid "6 dagar" (2+3+1)
    // TODO: beräkna baserat på faktisk kö och SLA-data
    private int calculateTotalEtaDays(ApplicationStatus currentStatus){
        return switch (currentStatus) {
            case PENDING_DOCS -> 6; // 2+3+1 — hardcoded
            case UNDER_REVIEW -> 4; // 3+1 — hardcoded
            default -> 1; // "1 dag" — hardcoded
        };

    }

    private enum StepStatus{
        DONE,
        PENDING,
        CURRENT,
    }


}
