package se.comerit.resurs.dto.backoffice;

import java.util.List;

public record BackOfficeListsDTO(
        List<HistoricalReviewInfo> decidedApplications,
        List<ReviewInfo> reviewApplications)
{}
