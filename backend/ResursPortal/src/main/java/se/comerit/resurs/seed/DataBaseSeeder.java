package se.comerit.resurs.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import se.comerit.resurs.dto.application.NewApplicationDTO;
import se.comerit.resurs.persistence.CaseWorkerRepository;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.CreditApplicationRepository;
import se.comerit.resurs.persistence.model.CaseWorker;
import se.comerit.resurs.persistence.model.Company;

import java.util.List;
import java.util.Set;

@Component
public class DataBaseSeeder implements CommandLineRunner {

    private final CaseWorkerRepository caseWorkerRepository;
    private final CompanyRepository companyRepository;
    private final CreditApplicationRepository applicationRepository;

    public DataBaseSeeder(CaseWorkerRepository caseWorkerRepository, CompanyRepository companyRepository, CreditApplicationRepository applicationRepository) {
        this.caseWorkerRepository = caseWorkerRepository;
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
    }


    @Override
    public void run(String... args) {
        // Insert development seed data here
    }

    private final List<Company> companySeed = List.of(
            new Company(),
            new Company(),
            new Company(),
            new Company(),
            new Company()
    );

    private final List<CaseWorker> workerSeed = List.of(
            new CaseWorker()
            );

    private final List<NewApplicationDTO> seedApplications = List.of(
      new NewApplicationDTO()
    );


}
