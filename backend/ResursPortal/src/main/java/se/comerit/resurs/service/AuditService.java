package se.comerit.resurs.service;

import se.comerit.resurs.dto.application.ApplicationCommentDTO;
import se.comerit.resurs.dto.audit.AuditDataFormat;
import se.comerit.resurs.dto.audit.ChainVerificationResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.AuditEventDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.enums.AuditAction;
import se.comerit.resurs.persistence.AuditEventRepository;
import se.comerit.resurs.persistence.model.AuditEvent;
import se.comerit.resurs.persistence.model.CreditApplication;

import java.util.List;
import java.util.Optional;

/**
 * AuditService -> skriver och läser audit-loggen för ärenden
 *
 * Ansvarar för: att spara en audit-händelse varje gång något viktigt sker på ett ärende (ansökan skapad, scoring körd,
 * dokument uppladdat, beslut fattat, kommentar tillagd), med ett ordningsnummer per ärende.
 * Den hämtar också ut kommentarerna för ett ärende ur loggen.
 *
 * Inte ansvarig för: att avgöra när något ska loggas eller vem som får läsa loggen -> det bestämmer de som anropar
 * t.ex BackofficeService och controllern.
 */
@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;
    private final AuditSigningService signingService;


    public AuditService (AuditEventRepository auditEventRepository,
                         ObjectMapper objectMapper, AuditSigningService signingService){
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
        this.signingService = signingService;
    }
    // TODO: previousHash, signingKeyId och signature fylls av signeringsmodulen
    //       (JNA, se v2-targets.md punkt 4). null tills den är på plats.
    private static final byte[] NOT_SIGNED_YET = null;

    private void saveEvent(CreditApplication application, AuditAction action, String actor, Object data) {

        AuditEvent newEvent = new AuditEvent(application, nextSequenceNumber(application.getId()),
                action, actor, objectMapper.valueToTree(data), NOT_SIGNED_YET, NOT_SIGNED_YET, null, NOT_SIGNED_YET);

        Optional<AuditEvent> previousEvent = auditEventRepository.findFirstByApplicationIdOrderBySequenceNumberDesc(application.getId());
        if (previousEvent.isEmpty()){
            newEvent.setPreviousHash(new byte[32]); // 32 zero bytes to mark first link in chain
        } else {
            newEvent.setPreviousHash(previousEvent.get().getCurrentHash()); //if previous auditevent in chain exists,  use its current hash
        }

        signingService.hashAndSign(newEvent);
        auditEventRepository.save(newEvent);
    }

    public void applicationCreated(CreditApplication application){
            saveEvent(application, AuditAction.APPLICATION_CREATED,
                    application.getCompany().getOrg_number(),
                    new AuditDataFormat.ApplicationCreated("COMPANY", application.getPurpose()));
    }

    public void scoringRun(CreditApplication application, int flags){
        saveEvent(application, AuditAction.SCORING_RUN, "SYSTEM",
                new AuditDataFormat.ScoringRun("SYSTEM", application.getDecision(),
                        application.getStatus(), flags));
    }

    public void documentUploaded(CreditApplication application, String fileName, String docType){
        saveEvent(application, AuditAction.DOCUMENT_UPLOADED,
                application.getCompany().getOrg_number(),
                new AuditDataFormat.DocumentUploaded("COMPANY", fileName, docType));
    }

    public void manualDecision(CreditApplication application,String workerEmail, String workerName,
                               ApplicationStatus previousStatus, String comment){
        String commentOrNull = comment == null || comment.isBlank() ? null : comment;
        saveEvent(application, AuditAction.MANUAL_DECISION, workerEmail,
                new AuditDataFormat.ManualDecision("CASE_WORKER", workerName, previousStatus,
                        application.getStatus(), commentOrNull));
    }

    private Long nextSequenceNumber(Long applicationID) {
        return auditEventRepository
                .findFirstByApplicationIdOrderBySequenceNumberDesc(applicationID)
                .map(event -> event.getSequenceNumber() + 1)
                .orElse(1L);
    }

    public List<AuditEventDTO> findAuditEventsByApplicationID(Long applicationID){
        return auditEventRepository.findByApplicationIdOrderBySequenceNumberAsc(applicationID)
                .stream().map(AuditEventDTO::new ).toList();
    }

    // Svarar ja/nej på om en handläggare har fattat ett beslut på ärendet
    public boolean hasManualDecision(Long applicationId) {
        return auditEventRepository.existsByApplicationIdAndAction(applicationId, AuditAction.MANUAL_DECISION);
    }

    public void commentAdded(CreditApplication application, String workerEmail, String workerName, String comment) {
        saveEvent(application, AuditAction.COMMENT_ADDED, workerEmail,
                new AuditDataFormat.CommentAdded("CASE_WORKER", workerName, comment));
    }

    public List<ApplicationCommentDTO> findComments(Long applicationId) {
        return auditEventRepository
                .findByApplicationIdAndActionInOrderBySequenceNumberAsc(applicationId,
                        List.of(AuditAction.MANUAL_DECISION, AuditAction.COMMENT_ADDED, AuditAction.DOCUMENTS_REQUESTED))
                .stream()
                .map(this::toComment)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ApplicationCommentDTO> toComment(AuditEvent event) {
        AuditDataFormat.CommentEntry entry = objectMapper.treeToValue(event.getData(),
                AuditDataFormat.CommentEntry.class);

        return Optional.ofNullable(entry.comment())
                .map(text -> new ApplicationCommentDTO(entry.workerName(), text, event.getOccurredAt()));
    }

    //Loggar att en handläggare bett kunden om fler dokument
    public void documentsRequested(CreditApplication application, String workerEmail, String workerName,
                                   ApplicationStatus previousStatus, String comment) {
        saveEvent(application, AuditAction.DOCUMENTS_REQUESTED, workerEmail,
                new AuditDataFormat.DocumentsRequested("CASE_WORKER", workerName, previousStatus, comment));
    }


    public ChainVerificationResponse verifyAuditChain(Long applicationID){
        List<AuditEvent> chain = auditEventRepository.findByApplicationIdOrderBySequenceNumberAsc(applicationID);
        return signingService.verifyChain(chain);
    }


}
