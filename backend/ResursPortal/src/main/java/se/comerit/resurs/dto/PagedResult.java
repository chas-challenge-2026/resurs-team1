package se.comerit.resurs.dto;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * PagedResult -> en "sida" med data, plus info om hur många sidor som finns totalt
 *
 * Istället för att skicka hela listan på en gång skickar vi bara en bit i taget (en sida),
 * och lägger med sidnummer, hur många som finns per sida, och totalt antal träffar och sidor.
 * Det gör att frontend kan visa "sida 2 av 5" eller en "Visa fler"-knapp.
 *
 * Representerar valfri typ T -> den vet inte vilken sorts data den skickar, den bara
 * paketerar den åt den som använder den.
 *
 */

public record PagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PagedResult<T> from(Page<T> page) {
        return new PagedResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
