package etude_cas_asin.asin_test.dto;

import java.time.LocalDateTime;

public record RendezVousResponse(
    Long id,
    String npi,
    String service,
    String dateHeure,
    String statut
) {}
