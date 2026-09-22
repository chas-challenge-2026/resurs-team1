package se.comerit.resurs.persistence.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column
    public String branchName;
    @Column(name = "bransch_faktor" )
    public double branchFactor;
    @Column
    public double branschSnittsSoliditet;
    @Column
    public double branschSnittSkuldsattning;
    @Column
    public double branschSnittMarginal;


}
