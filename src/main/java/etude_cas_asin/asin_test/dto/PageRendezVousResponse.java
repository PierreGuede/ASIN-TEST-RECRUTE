package etude_cas_asin.asin_test.dto;

import java.util.List;

public record PageRendezVousResponse(
    int page,
    int parPage,
    int total,
    List<RendezVousResponse> rendezVous
) {}
