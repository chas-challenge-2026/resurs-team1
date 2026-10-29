package se.comerit.resurs.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.comerit.resurs.dto.ContactDetails;
import se.comerit.resurs.dto.application.NewApplicationDTO;
import se.comerit.resurs.dto.caseworker.CreateCaseWorkerRequest;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.CaseWorkerRepository;
import se.comerit.resurs.persistence.model.Company;
import se.comerit.resurs.service.ApplicationService;
import se.comerit.resurs.service.CaseWorkerService;
import se.comerit.resurs.service.CompanyService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Profile("dev")
public class DataBaseSeeder implements CommandLineRunner {

    private final CaseWorkerService caseWorkerService;
    private final CaseWorkerRepository caseWorkerRepository;
    private final CompanyService companyService;
    private final ApplicationService applicationService;

    public DataBaseSeeder(CaseWorkerService caseWorkerService, CaseWorkerRepository caseWorkerRepository, CompanyService companyService, ApplicationService applicationService) {
        this.caseWorkerService = caseWorkerService;
        this.caseWorkerRepository = caseWorkerRepository;
        this.companyService = companyService;
        this.applicationService = applicationService;
    }


    //This runs after context is set up,
    // and as such we use the businesslogic in the services to ensure that our seed data is handled.
    //For example Password hashing and Audit log creation.
    @Override
    public void run(String... args) {

        //if database is empty, Seed it.
        if(caseWorkerRepository.existsById(1L)){
            return;
        }

        for (Company company:companySeed){
            companyService.createCompany(
                    company.getCompany_name(),
                    company.getOrg_number(),
                    company.getAuthorized_signatory()
            );
        }

        for(CreateCaseWorkerRequest worker:workerSeed){
            caseWorkerService.create(worker);
        }

        for(NewApplicationDTO creditApplication:seedApplications ){
            applicationService.saveApplication(creditApplication,new ContactDetails("Anders Karlsson","Anka@Email.se","707333333333"), false);
        }

        workerRoles.forEach((email,roles) -> {
            roles.forEach(role-> caseWorkerService.giveRole(email,role));
        });

    }

    private final List<Company> companySeed = List.of(
            new Company("556000-1234", "Fasen Elteknik AB", "Anders Karlsson"),
            new Company("556000-5678", "Britt Maries Ögonfransar AB", "Maria Svensson")
    );

    private final List<CreateCaseWorkerRequest> workerSeed = List.of(
            new CreateCaseWorkerRequest("Karin Handläggare", "karin@resurs.se", "password123"),
            new CreateCaseWorkerRequest("Charles Certifierad", "charles@resurs.se", "password456"),
            new CreateCaseWorkerRequest("Anders Admin", "anders@resurs.se", "password789")
    );

    private final Map<String,Set<String>>workerRoles = new HashMap(Map.of(
            "charles@resurs.se", Set.of("ROLE_CERTIFIED"),
            "anders@resurs.se", Set.of("ROLE_ADMIN")
    ));

    private final List<NewApplicationDTO> seedApplications = List.of(
        new NewApplicationDTO(
                BigDecimal.valueOf(500000.0),
                "Expansion av verksamheten",
                12, ApplicationStatus.UNDER_REVIEW,
                null,
                "'FLAGGED: soliditet=0.28 (OK), likviditetsgrad=0.95 (FLAGGED), skuldsättningsgrad=2.1 (OK)'",
                "'FLAGGED: soliditet=0.28 (OK), likviditetsgrad=0.95 (FLAGGED), skuldsättningsgrad=2.1 (OK)'",
                "If this can be read during runtime,  Something is wrong",
                "556000-1234",
                "Anders Karlsson",
                2)
    );



}
