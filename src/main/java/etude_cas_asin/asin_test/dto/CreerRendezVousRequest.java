package etude_cas_asin.asin_test.dto;

public record CreerRendezVousRequest(
    String npi,
    String service,
    String dateHeure
) {}
