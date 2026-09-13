package se.comerit.resurs.service;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import se.comerit.resurs.config.ScoringThresholds;
import se.comerit.resurs.persistence.BranchRepository;
import se.comerit.resurs.persistence.model.Branch;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@Testcontainers
class ScoringServiceTest {

    @Autowired
    private ScoringService service;

    @Autowired
    private ScoringThresholds thresholds;

    @Autowired
    private BranchRepository branchRepository;


    private static final Path SEED_SQL = Paths.get("").toAbsolutePath()
            .resolve("../../infra/seed.sql").normalize();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("resurs_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(SEED_SQL),
                            "/docker-entrypoint-initdb.d/seed.sql"
                    );


    // ============================================================
    // SOLIDITY
    // ============================================================

    @ParameterizedTest
    @MethodSource("solidityThresholds")
    void solidityThreshold(
            double value,
            boolean expectedHardReject,
            int expectedFlags
    ) {
        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.solidityCheck(
                value,
                BigDecimal.valueOf(100_000),
                state
        );

        assertEquals(expectedHardReject, state.isHardReject());
        assertEquals(expectedFlags, state.getFlagCount());
    }

    Stream<Arguments> solidityThresholds() {
        double minimum = thresholds.solidity().minimum();
        double low = thresholds.solidity().low();

        return Stream.of(
                Arguments.of(Math.nextDown(minimum), true, 0),
                Arguments.of(minimum, false, 1),
                Arguments.of(Math.nextDown(low), false, 1),
                Arguments.of(low, false, 0),
                Arguments.of(Math.nextUp(low), false, 0)
        );
    }


    // ============================================================
    // LARGE CREDIT + SOLIDITY
    // ============================================================

    @ParameterizedTest
    @MethodSource("largeCreditSolidityThresholds")
    void largeCreditSolidityThreshold(
            double soliditet,
            BigDecimal requestedAmount,
            int expectedFlags
    ) {
        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.solidityCheck(
                soliditet,
                requestedAmount,
                state
        );

        assertEquals(expectedFlags, state.getFlagCount());
    }

    Stream<Arguments> largeCreditSolidityThresholds() {
        double solidityThreshold = thresholds.solidity().low();
        double creditThreshold =
                thresholds.credit().high_credit_solidity();

        return Stream.of(
                Arguments.of(
                        Math.nextDown(solidityThreshold),
                        BigDecimal.valueOf(Math.nextUp(creditThreshold)),
                        2
                ),
                Arguments.of(
                        Math.nextDown(solidityThreshold),
                        BigDecimal.valueOf(creditThreshold),
                        1
                ),
                Arguments.of(
                        solidityThreshold,
                        BigDecimal.valueOf(Math.nextUp(creditThreshold)),
                        0
                )
        );
    }


    // ============================================================
    // LIQUIDITY
    // ============================================================

    @ParameterizedTest
    @MethodSource("liquidityThresholds")
    void liquidityThreshold(
            double value,
            int expectedFlags
    ) {
        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.liquidityCheck(value, state);

        assertEquals(expectedFlags, state.getFlagCount());
    }

     Stream<Arguments> liquidityThresholds() {
        double minimum = thresholds.liquidity().minimum();
        double low = thresholds.liquidity().low();
        double good = thresholds.liquidity().good();

        return Stream.of(
                Arguments.of(Math.nextDown(minimum), 1),
                Arguments.of(minimum, 0),
                Arguments.of(Math.nextDown(low), 1),
                Arguments.of(low, 0),
                Arguments.of(Math.nextDown(good), 0),
                Arguments.of(good, 0)
        );
    }


    // ============================================================
    // DEBT RATIO
    // ============================================================

    @ParameterizedTest
    @MethodSource("debtRatioThresholds")
    void debtRatioThreshold(
            double value,
            boolean expectedHardReject,
            int expectedFlags
    ) {
        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.checkDebtRatio(value, state);

        assertEquals(expectedHardReject, state.isHardReject());
        assertEquals(expectedFlags, state.getFlagCount());
    }

    Stream<Arguments> debtRatioThresholds() {
        double high = thresholds.debtRatio().high();
        double max = thresholds.debtRatio().max();

        return Stream.of(
                Arguments.of(Math.nextDown(high), false, 0),
                Arguments.of(high, false, 0),
                Arguments.of(Math.nextUp(high), false, 1),
                Arguments.of(Math.nextDown(max), false, 1),
                Arguments.of(max, false, 1),
                Arguments.of(Math.nextUp(max), true, 0)
        );
    }


    // ============================================================
    // OPERATING MARGIN
    // ============================================================

    @ParameterizedTest
    @MethodSource("operatingMarginThresholds")
    void operatingMarginThreshold(
            double value,
            int expectedFlags,
            int expectedScoreChange
    ) {
        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.checkOperatingMargin(value, state);

        assertEquals(expectedFlags, state.getFlagCount());
        assertEquals(
                expectedScoreChange,
                state.getPoints() - thresholds.initialScore()
        );
    }

    Stream<Arguments> operatingMarginThresholds() {
        double low = thresholds.operatingMargin().low();
        double good = thresholds.operatingMargin().good();

        return Stream.of(
                Arguments.of(Math.nextDown(low), 1, -10),
                Arguments.of(low, 0, 0),
                Arguments.of(Math.nextDown(good), 0, 0),
                Arguments.of(good, 0, 8)
        );
    }


    // ============================================================
    // CASHFLOW RATIO
    // ============================================================

    @ParameterizedTest
    @MethodSource("cashflowThresholds")
    void cashflowThreshold(
            double value,
            boolean expectedHardReject,
            int expectedFlags,
            int expectedScoreChange
    ) {
        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.cashflowCheck(
                value,
                1_000_000,
                0,
                state
        );

        assertEquals(expectedHardReject, state.isHardReject());
        assertEquals(expectedFlags, state.getFlagCount());
        assertEquals(
                expectedScoreChange,
                state.getPoints() - thresholds.initialScore()
        );
    }

    Stream<Arguments> cashflowThresholds() {
        double veryLow = thresholds.cashflowRatio().very_low();
        double low = thresholds.cashflowRatio().low();

        return Stream.of(
                Arguments.of(Math.nextDown(0.0), true, 0, -30),
                Arguments.of(0.0, false, 1, -12),
                Arguments.of(Math.nextDown(veryLow), false, 1, -12),
                Arguments.of(veryLow, false, 1, -6),
                Arguments.of(Math.nextDown(low), false, 1, -6),
                Arguments.of(low, false, 0, 5)
        );
    }


    // ============================================================
    // INVESTMENT CASHFLOW
    // ============================================================

    @ParameterizedTest
    @MethodSource("investmentCashflowThresholds")
    void investmentCashflowThreshold(
            double investmentCashflow,
            int expectedFlags
    ) {
        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.cashflowCheck(
                0.10,
                1_000_000,
                investmentCashflow,
                state
        );

        assertEquals(expectedFlags, state.getFlagCount());
    }

    Stream<Arguments> investmentCashflowThresholds() {
        double turnover = 1_000_000;

        double threshold =
                turnover *
                        thresholds.investmentCashFlow().inverse_cashflow_flag();

        double boundary = -threshold;

        return Stream.of(
                Arguments.of(Math.nextDown(boundary), 1),
                Arguments.of(boundary, 0),
                Arguments.of(Math.nextUp(boundary), 0)
        );
    }


    // ============================================================
    // INTEREST COVERAGE
    // ============================================================

    @ParameterizedTest
    @MethodSource("interestCoverageThresholds")
    void interestCoverageThreshold(
            double value,
            boolean expectedHardReject,
            int expectedFlags,
            int expectedScoreChange
    ) {
        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.interestCoverageCheck(value, state);

        assertEquals(expectedHardReject, state.isHardReject());
        assertEquals(expectedFlags, state.getFlagCount());
        assertEquals(
                expectedScoreChange,
                state.getPoints() - thresholds.initialScore()
        );
    }

    Stream<Arguments> interestCoverageThresholds() {
        double reject = thresholds.interestCoverage().reject();
        double low = thresholds.interestCoverage().low();

        return Stream.of(
                Arguments.of(Math.nextDown(reject), true, 0, -35),
                Arguments.of(reject, false, 1, -15),
                Arguments.of(Math.nextDown(low), false, 1, -15),
                Arguments.of(low, false, 0, 8)
        );
    }


    // ============================================================
    // EQUITY COVERAGE
    // ============================================================

    @ParameterizedTest
    @MethodSource("equityCoverageThresholds")
    void equityCoverageThreshold(
            double equityRatio,
            int expectedFlags
    ) {
        BigDecimal requestedAmount =
                BigDecimal.valueOf(100_000);

        double equity =
                requestedAmount.doubleValue() * equityRatio;

        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.checkLowCapitalCreditRatio(
                requestedAmount,
                equity,
                state
        );

        assertEquals(expectedFlags, state.getFlagCount());
    }

    Stream<Arguments> equityCoverageThresholds() {
        double threshold =
                thresholds.equityCoverage().credit_quotient();

        return Stream.of(
                Arguments.of(Math.nextDown(threshold), 1),
                Arguments.of(threshold, 0),
                Arguments.of(Math.nextUp(threshold), 0)
        );
    }


    // ============================================================
    // DEBT ACKNOWLEDGEMENT ERROR
    // ============================================================

    @ParameterizedTest
    @MethodSource("debtAcknowledgementThresholds")
    void debtAcknowledgementThreshold(
            double ratio,
            int expectedFlags
    ) {
        double turnover = 1_000_000;

        double totalDebt =
                ratio * (turnover + 1);

        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.checkDebtAcknowledgementError(
                totalDebt,
                0,
                turnover,
                state
        );

        assertEquals(expectedFlags, state.getFlagCount());
    }

    Stream<Arguments> debtAcknowledgementThresholds() {
        double threshold =
                thresholds.debtAckError().high();

        return Stream.of(
                Arguments.of(Math.nextDown(threshold), 0),
                Arguments.of(threshold, 0),
                Arguments.of(Math.nextUp(threshold), 1)
        );
    }


    // ============================================================
    // LOW SOLIDITY + HIGH DEBT
    // ============================================================

    @ParameterizedTest
    @MethodSource("combinationThresholds")
    void lowSolidityHighDebtThreshold(
            double solidity,
            double debtRatio,
            int expectedFlags
    ) {
        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.checkLowSolidityHighDebtRatio(
                solidity,
                debtRatio,
                state
        );

        assertEquals(expectedFlags, state.getFlagCount());
    }

    Stream<Arguments> combinationThresholds() {
        double solidity = thresholds.solidity().low();
        double debt = thresholds.debtRatio().high();

        return Stream.of(
                Arguments.of(
                        Math.nextDown(solidity),
                        Math.nextUp(debt),
                        1
                ),
                Arguments.of(
                        solidity,
                        Math.nextUp(debt),
                        0
                ),
                Arguments.of(
                        Math.nextDown(solidity),
                        debt,
                        0
                )
        );
    }


    // ============================================================
    // LOW LIQUIDITY + NEGATIVE RESULT
    // ============================================================

    @ParameterizedTest
    @MethodSource("lowLiquidityThresholds")
    void lowLiquidityNegativeResultThreshold(
            double liquidity,
            double operatingResult,
            boolean expectedReject
    ) {
        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.checkLowLiquidityNegativeOperational(
                liquidity,
                operatingResult,
                state
        );

        assertEquals(expectedReject, state.isHardReject());
    }

    Stream<Arguments> lowLiquidityThresholds() {
        double minimum = thresholds.liquidity().minimum();

        return Stream.of(
                Arguments.of(Math.nextDown(minimum), -1.0, true),
                Arguments.of(minimum, -1.0, false),
                Arguments.of(Math.nextDown(minimum), 0.0, false)
        );
    }


    // ============================================================
    // CREDIT ABOVE TURNOVER
    // ============================================================

    @ParameterizedTest
    @MethodSource("creditTurnoverBoundaryValues")
    void creditAboveTurnoverBoundary(
            BigDecimal requestedAmount,
            double turnover,
            int expectedFlags
    ) {
        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.checkCreditAboveTurnover(
                requestedAmount,
                turnover,
                state
        );

        assertEquals(expectedFlags, state.getFlagCount());
    }

    Stream<Arguments> creditTurnoverBoundaryValues() {
        double requestedAmount = 1_000_000;

        return Stream.of(
                Arguments.of(
                        BigDecimal.valueOf(Math.nextDown(requestedAmount)),
                        requestedAmount,
                        0
                ),
                Arguments.of(
                        BigDecimal.valueOf(requestedAmount),
                        requestedAmount,
                        0
                ),
                Arguments.of(
                        BigDecimal.valueOf(Math.nextUp(requestedAmount)),
                        requestedAmount,
                        1
                )
        );
    }


    // ============================================================
    // CASHFLOW + DEBT
    // ============================================================

    @ParameterizedTest
    @MethodSource("cashflowDebtThresholds")
    void cashflowDebtThreshold(
            double cashflow,
            double debt,
            int expectedFlags
    ) {
        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.checkCashFlowToDebtRatio(
                cashflow,
                debt,
                state
        );

        assertEquals(expectedFlags, state.getFlagCount());
    }

    Stream<Arguments> cashflowDebtThresholds() {
        double cashflow =
                thresholds.cashflowRatio().very_low();

        double debt =
                thresholds.debtRatio().high();

        return Stream.of(
                Arguments.of(
                        Math.nextDown(cashflow),
                        Math.nextUp(debt),
                        1
                ),
                Arguments.of(
                        cashflow,
                        Math.nextUp(debt),
                        0
                ),
                Arguments.of(
                        Math.nextDown(cashflow),
                        debt,
                        0
                )
        );
    }


    // ============================================================
    // BRANCH THRESHOLDS
    // ============================================================

    @ParameterizedTest
    @MethodSource("branchMarginThresholds")
    void branchMarginThreshold(
            double margin,
            int expectedFlags
    ) {
        Branch branch = branchRepository
                .findByBranchName("BYGG")
                .orElseThrow();

        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.branchAdjustedChecks(
                branch.branchName,
                margin,
                0.30,
                state
        );

        assertEquals(expectedFlags, state.getFlagCount());
    }

    Stream<Arguments> branchMarginThresholds() {
        Branch branch = branchRepository
                .findByBranchName("BYGG")
                .orElseThrow();

        double branchMargin = branch.branschSnittMarginal;

        double threshold =
                branchMargin *
                        thresholds.operatingMargin().flag_below_branch_avg();

        return Stream.of(
                Arguments.of(Math.nextDown(threshold), 1),
                Arguments.of(threshold, 0),
                Arguments.of(Math.nextUp(threshold), 0)
        );
    }



    @ParameterizedTest
    @MethodSource("branchSolidityThresholds")
    void branchSolidityThreshold(
            double solidity,
            int expectedFlags
    ) {
        Branch branch = branchRepository
                .findByBranchName("BYGG")
                .orElseThrow();

        when(branchRepository.findByBranchName("BYGG"))
                .thenReturn(Optional.of(branch));

        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.branchAdjustedChecks(
                branch.branchName,
                0.04,
                solidity,
                state
        );

        assertEquals(expectedFlags, state.getFlagCount());
    }

    Stream<Arguments> branchSolidityThresholds() {
        Branch branch = branchRepository
                .findByBranchName("BYGG")
                .orElseThrow();

        double branchMargin = branch.branschSnittMarginal;

        double threshold =
                branchMargin *
                        thresholds.operatingMargin().flag_below_branch_avg();

        return Stream.of(
                Arguments.of(Math.nextDown(threshold), 1),
                Arguments.of(threshold, 0),
                Arguments.of(Math.nextUp(threshold), 0)
        );
    }


    @ParameterizedTest
    @MethodSource("branchFactorThresholds")
    void branchFactorThreshold(
            double solidity,
            int expectedFlags
    ) {
        Branch branch = branchRepository
                .findByBranchName("BYGG")
                .orElseThrow();


        ScoringState state =
                new ScoringState(thresholds.initialScore());

        service.branchAdjustedChecks(
                branch.branchName,
                0.04,
                solidity,
                state
        );

        assertEquals(expectedFlags, state.getFlagCount());
    }

    Stream<Arguments> branchFactorThresholds() {
        double branchFactor = 0.85;

        double threshold =
                thresholds.solidity().minimum() *
                        branchFactor;

        return Stream.of(
                Arguments.of(Math.nextDown(threshold), 1),
                Arguments.of(threshold, 0),
                Arguments.of(Math.nextUp(threshold), 0)
        );
    }


    // ============================================================
    // FINAL SCORE
    // ============================================================

    @ParameterizedTest
    @MethodSource("finalScoreThresholds")
    void finalScoreThreshold(
            int score,
            boolean hardReject,
            String expectedDecision
    ) {
        ScoringState state =
                new ScoringState(thresholds.initialScore());

        state.removePoints(score);
        state.setHardReject(hardReject);

        ScoringService.Decision decision =
                service.finalDecision(state);

        assertEquals(
                expectedDecision,
                decision.finalDecision()
        );
    }

    Stream<Arguments> finalScoreThresholds() {
        int reject = thresholds.finalScoreThresholds().reject();
        int manualReview = thresholds.finalScoreThresholds().manual_review();
        int flagReview = thresholds.finalScoreThresholds().flag_review();

        return Stream.of(
                Arguments.of(reject - 1, false, "REJECTED"),
                Arguments.of(reject, false, "REVIEW"),
                Arguments.of(manualReview - 1, false, "REVIEW"),
                Arguments.of(manualReview, false, "REVIEW"),
                Arguments.of(flagReview - 1, false, "REVIEW"),
                Arguments.of(flagReview, false, "APPROVED"),
                Arguments.of(
                        thresholds.initialScore(),
                        true,
                        "REJECTED"
                )
        );
    }
}

