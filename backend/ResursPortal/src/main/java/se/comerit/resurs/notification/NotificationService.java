package se.comerit.resurs.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final EmailSender emailSender;

    public NotificationService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    // Körs först när transaktionen har sparats, så vi aldrig mejlar om något som rullats tillbaka
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(ApplicationStatusChangedEvent event) {
        if (event.email() == null || event.email().isBlank()) {
            return;
        }
        try {
            emailSender.send(event.email(), "Resurs: ansökan #" + event.applicationId(), bodyFor(event));
        } catch (RuntimeException e) {
            // Ett mejlfel får aldrig påverka ansökan, så vi loggar bara
            log.warn("Could not send e-mail for application {}", event.applicationId(), e);
        }
    }

    // Ingen motivering eller belopp i mejlet, kunden vill dela så lite känslig data som möjligt
    private String bodyFor(ApplicationStatusChangedEvent event) {
        String statusText = switch (event.status()) {
            case PENDING_SCORING, UNDER_REVIEW -> "är mottagen och granskas";
            case PENDING_DOCS -> "behöver kompletteras, logga in för att se vad som saknas";
            case APPROVED -> "har godkänts";
            case REJECTED -> "har avslagits";
        };
        return "Hej! Din ansökan #" + event.applicationId() + " " + statusText
                + ".\n\nLogga in i portalen för mer information.";
    }
}