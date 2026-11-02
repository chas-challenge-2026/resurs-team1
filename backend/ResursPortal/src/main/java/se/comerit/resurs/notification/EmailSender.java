package se.comerit.resurs.notification;

public interface EmailSender {
    void send(String to, String subject, String body);
}
