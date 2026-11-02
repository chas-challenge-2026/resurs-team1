package se.comerit.resurs.notification;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.comerit.resurs.enums.ApplicationStatus;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class NotificationServiceTest {

    private final EmailSender emailSender = Mockito.mock(EmailSender.class);
    private final NotificationService service = new NotificationService(emailSender);

    // En godkänd ansökan ska ge ett mejl till rätt adress
    @Test
    void shouldSendEmailWhenApplicationIsApproved() {
        service.onStatusChanged(new ApplicationStatusChangedEvent(
                7L, "kund@example.com", ApplicationStatus.APPROVED));

        verify(emailSender).send(eq("kund@example.com"), contains("#7"), contains("godkänts"));
    }

    // Saknas adress ska inget skickas
    @Test
    void shouldNotSendWhenEmailIsMissing() {
        service.onStatusChanged(new ApplicationStatusChangedEvent(
                7L, null, ApplicationStatus.APPROVED));

        verifyNoInteractions(emailSender);
    }

    // Ett fel vid sändning får inte kasta vidare, annars kan ansökan påverkas
    @Test
    void shouldNotThrowWhenSendingFails() {
        doThrow(new RuntimeException("smtp down")).when(emailSender).send(anyString(), anyString(), anyString());

        assertThatCode(() -> service.onStatusChanged(
                new ApplicationStatusChangedEvent(
                        7L, "kund@example.com", ApplicationStatus.REJECTED)))
                .doesNotThrowAnyException();
    }
}