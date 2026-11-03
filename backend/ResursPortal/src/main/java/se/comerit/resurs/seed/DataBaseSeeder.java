package se.comerit.resurs.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.comerit.resurs.dto.ContactDetails;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.CreditApplicationDataObject;
import se.comerit.resurs.dto.application.NewApplicationDTO;
import se.comerit.resurs.dto.caseworker.CreateCaseWorkerRequest;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.CaseWorkerRepository;
import se.comerit.resurs.persistence.model.Company;
import se.comerit.resurs.service.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Profile("dev")
public class DataBaseSeeder implements CommandLineRunner {

    private final CaseWorkerService caseWorkerService;
    private final ScoringService scoringService;
    private final CaseWorkerRepository caseWorkerRepository;
    private final CompanyService companyService;
    private final ApplicationService applicationService;
    private final CompanyFinancialService financialService;

    public DataBaseSeeder(CaseWorkerService caseWorkerService, ScoringService scoringService, CaseWorkerRepository caseWorkerRepository, CompanyService companyService, ApplicationService applicationService, CompanyFinancialService financialService) {
        this.caseWorkerService = caseWorkerService;
        this.scoringService = scoringService;
        this.caseWorkerRepository = caseWorkerRepository;
        this.companyService = companyService;
        this.applicationService = applicationService;
        this.financialService = financialService;
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

        for (ApplicationSeed creditApplication : createApplicationSeeds()) {
            NewApplicationDTO scoredApplication = scoringService.scoreFromFinancialObject(
                    creditApplication.financials(),
                    creditApplication.requestedAmount(),
                    creditApplication.bransch(),
                    creditApplication.orgNumber(),
                    creditApplication.companyName(),
                    creditApplication.authorizedSignatory(),
                    creditApplication.purpose(),
                    creditApplication.durationMonths()
            );

            CreditApplicationDataObject object = applicationService.saveApplication(
                    scoredApplication,
                    creditApplication.contactDetails,
                    creditApplication.protectedResource
            );

            System.out.println(object);
        }

        workerRoles.forEach((email,roles) -> {
            roles.forEach(role-> caseWorkerService.giveRole(email,role));
        });

    }

    private final List<Company> companySeed = List.of(
            new Company("556000-1234", "Fasen Elteknik AB", "Anders Karlsson"),
            new Company("556000-5678", "Britt Maries Ögonfransar AB", "Maria Svensson"),
            new Company("556000-9999", "Frukt och grönt Göteborg", "Erik Lindqvist"),
            new Company("556000-7777", "Gunnar Kruts Dynamit AB", "Johan Berg")
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


    private List<ApplicationSeed> createApplicationSeeds(){
        return List.of(
                new ApplicationSeed(financialService.fetchLatestAnnualReport("556000-1234").get(), //succeeds
                        BigDecimal.valueOf(500000.0),
                        "35.400 - Mäklare och agenter för el och naturgas",
                        "556000-1234",
                        "Fasen Elteknik AB",
                        "750312-1234",
                        "Expansion av verksamhet", 6, false,
                        new ContactDetails("Anders Karlsson", "anders@fasenel.se", "0701234567")
                        ),
                new ApplicationSeed(financialService.fetchLatestAnnualReport("556000-9999").get(), //Protected identity on signatory
                        BigDecimal.valueOf(20000.0),
                        "46.310 - Partihandel med frukt och grönsaker",
                        "556000-9999",
                        "Frukt och grönt Göteborg",
                        "681105-9999",
                        "Ny butik",12, true,
                        new ContactDetails("Erik Lindqvist", "erik@fruktgbg.se", "0702345678")),
                new ApplicationSeed(financialService.fetchLatestAnnualReport("556000-7777").get(),
                        BigDecimal.valueOf(1500000),
                        "20.591 - Sprängämnestillverkning",
                        "556000-7777",
                        "Gunnar Kruts Dynamit AB",
                        "660930-7777",
                        "Maskininvestering", 6, false,
                        new ContactDetails("Johan Berg", "johan@kruts.se", "0703456789")
                )
        );
    }

    private record ApplicationSeed(
            CompanyFinancialApiDTO financials,
            BigDecimal requestedAmount,
            String bransch,
            String orgNumber,
            String companyName,
            String authorizedSignatory,
            String purpose,
            Integer durationMonths,
            boolean protectedResource,
            ContactDetails contactDetails
    ) {}

}
