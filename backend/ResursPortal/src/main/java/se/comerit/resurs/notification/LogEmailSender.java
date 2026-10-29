package se.comerit.resurs.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "notification", name = "client", havingValue = "log", matchIfMissing = true)
class LogEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        // Mottagarens adress är en personuppgift, så den loggas inte
        log.info("[MOCK E-MAIL] {} | {}", subject, body);
    }
}