package se.comerit.resurs.persistence.model;


import jakarta.persistence.*;
import se.comerit.resurs.persistence.converter.PiiAttributeConverter;
import se.comerit.resurs.persistence.listener.SignatureSearchKeyListener;

@Entity
@EntityListeners(SignatureSearchKeyListener.class)
@Table(name = "signatories")
public class Signatory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "personal_number", nullable = false)
    @Convert(converter = PiiAttributeConverter.class)
    private String personalNumber;

    @Column(name = "search_key_personal_number", nullable = false)
    private byte[]searchKeyPersonalNumber;

    @Column(name = "name", nullable = false)
    @Convert(converter = PiiAttributeConverter.class)
    private String name;


    protected Signatory(){}

    public Signatory(String personalNumber, String name){
        this.personalNumber = personalNumber;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPersonalNumber() {
        return personalNumber;
    }

    public byte[] getSearchKeyPersonalNumber() {
        return searchKeyPersonalNumber;
    }

    public void setSearchKeyPersonalNumber(byte[] searchKeyPersonalNumber) {
        this.searchKeyPersonalNumber = searchKeyPersonalNumber;
    }
}
