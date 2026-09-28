package se.comerit.resurs.dto.backoffice;

import se.comerit.resurs.dto.PagedResult;

/**
 * BackOfficeListsDTO -> det som skickas till handläggarens översiktssida
 *
 * Innehåller två listor, en sida i taget: ansökningar som väntar på granskning
 * (reviewApplications) och ansökningar som redan är avgjorda (decidedApplications).
 *
 * Representerar bara data som ska skickas -> ingen logik här, byggs av BackofficeService.
 *
 */

public record BackOfficeListsDTO(
        PagedResult<HistoricalReviewInfo> decidedApplications,
        PagedResult<ReviewInfo> reviewApplications)
{}
