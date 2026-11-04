package se.comerit.resurs.exception.companyvalidation;

public class CompanyRegistryUnavailableException extends RuntimeException {
    private final String registry;
    private final String orgNumber;

    public CompanyRegistryUnavailableException(String registry, String orgNumber, Throwable cause) {
        super(registry + " svarade inte för " + orgNumber, cause);
        this.registry = registry;
        this.orgNumber = orgNumber;
    }

    public String registry() { return registry; }
    public String orgNumber() { return orgNumber; }
}

