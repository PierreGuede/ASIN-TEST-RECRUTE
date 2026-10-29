package etude_cas_asin.asin_test.service;

import etude_cas_asin.asin_test.dto.CreerRendezVousRequest;
import etude_cas_asin.asin_test.dto.PageRendezVousResponse;
import etude_cas_asin.asin_test.dto.RendezVousResponse;
import etude_cas_asin.asin_test.exception.ErreurMetierException;
import etude_cas_asin.asin_test.model.RendezVous;
import etude_cas_asin.asin_test.model.ServiceType;
import etude_cas_asin.asin_test.model.Statut;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReglesGestionService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final LocalTime PREMIER_CRENEAU = LocalTime.of(8, 0);
    private static final LocalTime DERNIER_CRENEAU = LocalTime.of(15, 30);
    private static final int MAX_PAR_CRENEAU = 3;
    private static final int PAR_PAGE = 10;

    private final Map<Long, RendezVous> rendezVousStore = new ConcurrentHashMap<>();
    private long nextId = 1;
    private LocalDateTime maintenant;

    public ReglesGestionService(LocalDateTime maintenant) {
        this.maintenant = maintenant;
    }

    public void setMaintenant(LocalDateTime maintenant) {
        this.maintenant = maintenant;
    }

    public RendezVousResponse creerRendezVous(CreerRendezVousRequest request) {
        validerNPI(request.npi());
        ServiceType service = validerService(request.service());
        LocalDateTime dateHeure = validerDateHeure(request.dateHeure());

        verifierWeekend(dateHeure);
        verifierCreneauValide(dateHeure);
        verifierDatePassee(dateHeure);
        verifierPasDeRendezVousAVenir(request.npi());
        verifierCapaciteCreneau(dateHeure);

        RendezVous rendezVous = new RendezVous(
            nextId++,
            request.npi(),
            service,
            dateHeure,
            Statut.CONFIRME,
            maintenant
        );
        rendezVousStore.put(rendezVous.getId(), rendezVous);

        return toResponse(rendezVous);
    }

    public void annulerRendezVous(Long id) {
        RendezVous rendezVous = trouverRendezVous(id);

        if (rendezVous.getStatut() != Statut.CONFIRME) {
            throw new ErreurMetierException(409, "Ce rendez-vous ne peut plus être annulé.");
        }

        long heuresAvant = ChronoUnit.HOURS.between(maintenant, rendezVous.getDateHeure());
        if (heuresAvant < 24) {
            throw new ErreurMetierException(409, "Annulation impossible moins de 24 h avant le rendez-vous.");
        }

        rendezVous.setStatut(Statut.ANNULE);
    }

    public RendezVousResponse changerStatut(Long id, String nouveauStatutStr) {
        RendezVous rendezVous = trouverRendezVous(id);

        Statut nouveauStatut = validerStatut(nouveauStatutStr);

        if (rendezVous.getStatut().isDefinitif()) {
            throw new ErreurMetierException(409, "Ce rendez-vous est déjà clôturé.");
        }

        if (nouveauStatut.isDefinitif() && rendezVous.getStatut() != Statut.CONFIRME) {
            throw new ErreurMetierException(409, "Ce rendez-vous est déjà clôturé.");
        }

        rendezVous.setStatut(nouveauStatut);

        return toResponse(rendezVous);
    }

    public PageRendezVousResponse listerRendezVousUsager(String npi, int page) {
        List<RendezVous> rendezVousList = rendezVousStore.values().stream()
            .filter(r -> r.getNpi().equals(npi))
            .sorted(Comparator.comparing(RendezVous::getDateHeure).reversed())
            .collect(Collectors.toList());

        int total = rendezVousList.size();
        int startIndex = (page - 1) * PAR_PAGE;
        int endIndex = Math.min(startIndex + PAR_PAGE, total);

        List<RendezVousResponse> pageRendezVous = rendezVousList.subList(
            startIndex >= total ? 0 : startIndex,
            endIndex
        ).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return new PageRendezVousResponse(page, PAR_PAGE, total, pageRendezVous);
    }

    private void validerNPI(String npi) {
        if (npi == null || !npi.matches("\\d{10}")) {
            throw new ErreurMetierException(400, "Le NPI doit comporter exactement 10 chiffres.");
        }
    }

    private ServiceType validerService(String service) {
        ServiceType serviceType = ServiceType.fromString(service);
        if (serviceType == null) {
            throw new ErreurMetierException(400, "Service inconnu : choisissez PASSEPORT, CNI ou PERMIS.");
        }
        return serviceType;
    }

    private LocalDateTime validerDateHeure(String dateHeure) {
        if (dateHeure == null) {
            throw new ErreurMetierException(400, "Date et heure invalides (format attendu : AAAA-MM-JJTHH:MM).");
        }

        try {
            DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            return LocalDateTime.parse(dateHeure, inputFormat);
        } catch (DateTimeParseException e) {
            throw new ErreurMetierException(400, "Date et heure invalides (format attendu : AAAA-MM-JJTHH:MM).");
        }
    }

    private Statut validerStatut(String statut) {
        Statut statutEnum = Statut.fromString(statut);
        if (statutEnum == null || statutEnum == Statut.CONFIRME || statutEnum == Statut.ANNULE) {
            throw new ErreurMetierException(400, "Statut invalide : honore ou absent.");
        }
        return statutEnum;
    }

    private void verifierWeekend(LocalDateTime dateHeure) {
        DayOfWeek jour = dateHeure.getDayOfWeek();
        if (jour == DayOfWeek.SATURDAY || jour == DayOfWeek.SUNDAY) {
            throw new ErreurMetierException(409, "Le centre est fermé le week-end.");
        }
    }

    private void verifierCreneauValide(LocalDateTime dateHeure) {
        LocalTime heure = dateHeure.toLocalTime();
        int minutes = heure.getMinute();

        if (minutes % 30 != 0) {
            throw new ErreurMetierException(409, "Créneau invalide : de 08:00 à 15:30, toutes les 30 minutes.");
        }

        if (heure.isBefore(PREMIER_CRENEAU) || heure.isAfter(DERNIER_CRENEAU)) {
            throw new ErreurMetierException(409, "Créneau invalide : de 08:00 à 15:30, toutes les 30 minutes.");
        }
    }

    private void verifierDatePassee(LocalDateTime dateHeure) {
        if (dateHeure.isBefore(maintenant)) {
            throw new ErreurMetierException(409, "Impossible de prendre un rendez-vous dans le passé.");
        }
    }

    private void verifierPasDeRendezVousAVenir(String npi) {
        boolean aRendezVousAVenir = rendezVousStore.values().stream()
            .filter(r -> r.getNpi().equals(npi))
            .anyMatch(r -> r.estAVenir(maintenant) && r.getStatut() != Statut.ANNULE);

        if (aRendezVousAVenir) {
            throw new ErreurMetierException(409, "Vous avez déjà un rendez-vous à venir.");
        }
    }

    private void verifierCapaciteCreneau(LocalDateTime dateHeure) {
        long count = rendezVousStore.values().stream()
            .filter(r -> r.getDateHeure().equals(dateHeure))
            .filter(r -> r.getStatut() != Statut.ANNULE)
            .count();

        if (count >= MAX_PAR_CRENEAU) {
            throw new ErreurMetierException(409, "Ce créneau est complet.");
        }
    }

    private RendezVous trouverRendezVous(Long id) {
        RendezVous rendezVous = rendezVousStore.get(id);
        if (rendezVous == null) {
            throw new ErreurMetierException(404, "Rendez-vous non trouvé.");
        }
        return rendezVous;
    }

    private RendezVousResponse toResponse(RendezVous rendezVous) {
        return new RendezVousResponse(
            rendezVous.getId(),
            rendezVous.getNpi(),
            rendezVous.getService().name(),
            rendezVous.getDateHeure().format(DATE_FORMAT),
            rendezVous.getStatut().getValue()
        );
    }

    public void ajouterRendezVous(RendezVous rendezVous) {
        rendezVousStore.put(rendezVous.getId(), rendezVous);
        if (rendezVous.getId() >= nextId) {
            nextId = rendezVous.getId() + 1;
        }
    }

    public void clear() {
        rendezVousStore.clear();
        nextId = 1;
    }
}
