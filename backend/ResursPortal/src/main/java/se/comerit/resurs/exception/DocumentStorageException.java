package se.comerit.resurs.exception;

public class DocumentStorageException extends RuntimeException {
    public DocumentStorageException(String message,Exception e) {
        super(message, e);
    }
}
