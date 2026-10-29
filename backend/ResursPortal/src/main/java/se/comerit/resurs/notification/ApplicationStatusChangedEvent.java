package se.comerit.resurs.notification;

import se.comerit.resurs.enums.ApplicationStatus;

public record ApplicationStatusChangedEvent(Long applicationId, String email, ApplicationStatus status) {}
