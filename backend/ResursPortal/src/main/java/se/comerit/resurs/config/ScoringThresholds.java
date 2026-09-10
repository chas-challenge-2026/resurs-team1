package se.comerit.resurs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix ="scoring")
public record ScoringThresholds(
        SolidityThresholds solidity,
        LiquidityThresholds liquidity,
        DebtRatioThresholds debtRatio,
        OperatingMarginThresholds operatingMargin,
        HighCreditThresholds credit,
        TurnoverThresholds turnover,
        CashFlowRatioThresholds cashflowRatio,
        InvestmentCashFlowThreshold investmentCashFlow,
        InterestCoverageThreshold interestCoverage,
        EquityCoverageThreshold equityCoverage,
        DebtAcknowledgementError debtAckError,
        CreditScoreThresholds finalScoreThresholds,
        int initialScore

) {
    public record SolidityThresholds(
            double low,
            double minimum,
            double flag_below_branch_avg
    ){}
    public record LiquidityThresholds(
            double good,
            double low,
            double minimum
    ){}
    public record DebtRatioThresholds(
            double max,
            double high
    ){}
    public record OperatingMarginThresholds(
            double low,
            double good,
            double flag_below_branch_avg
    ){}
    public record HighCreditThresholds(
            double high_credit_solidity,
            double extreme_credit
    ){}
    public record TurnoverThresholds(
        double low
    ){}
    public record CashFlowRatioThresholds(
        double low,
        double very_low
    ){}
    public record InvestmentCashFlowThreshold(
        double inverse_cashflow_flag
    ){}
    public record InterestCoverageThreshold(
        double reject,
        double low
    ){}
    public record EquityCoverageThreshold(
        double credit_quotient
    ){}
    public record DebtAcknowledgementError(
        double high
    ){}
    public record CreditScoreThresholds(
            int manual_review,
            int flag_review,
            int reject
    ){}



}
