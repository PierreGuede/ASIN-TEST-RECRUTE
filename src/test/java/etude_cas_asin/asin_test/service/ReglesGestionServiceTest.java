package etude_cas_asin.asin_test.service;

import etude_cas_asin.asin_test.dto.CreerRendezVousRequest;
import etude_cas_asin.asin_test.dto.PageRendezVousResponse;
import etude_cas_asin.asin_test.dto.RendezVousResponse;
import etude_cas_asin.asin_test.exception.ErreurMetierException;
import etude_cas_asin.asin_test.model.RendezVous;
import etude_cas_asin.asin_test.model.ServiceType;
import etude_cas_asin.asin_test.model.Statut;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReglesGestionServiceTest {

    private ReglesGestionService service;
    private LocalDateTime maintenant;

    @BeforeEach
    void setUp() {
        maintenant = LocalDateTime.of(2026, 11, 20, 10, 0);
        service = new ReglesGestionService(maintenant);
    }

    @Test
    void testTMA101_RefuserRendezVousSamedi() {
        CreerRendezVousRequest request = new CreerRendezVousRequest(
            "0614253647",
            "PASSEPORT",
            "2026-11-14T10:00"
        );

        ErreurMetierException exception = assertThrows(ErreurMetierException.class, () -> {
            service.creerRendezVous(request);
        });

        assertEquals(409, exception.getStatusCode());
        assertEquals("Le centre est fermé le week-end.", exception.getMessage());
    }

    @Test
    void testTMA102_LimiterTroisParCreneau() {
        LocalDateTime creneau = LocalDateTime.of(2026, 11, 24, 9, 0);

        for (int i = 0; i < 3; i++) {
            CreerRendezVousRequest request = new CreerRendezVousRequest(
                String.format("012345678%d", i),
                "CNI",
                "2026-11-24T09:00"
            );
            RendezVousResponse response = service.creerRendezVous(request);
            assertNotNull(response);
        }

        CreerRendezVousRequest request4 = new CreerRendezVousRequest(
            "0987654321",
            "CNI",
            "2026-11-24T09:00"
        );

        ErreurMetierException exception = assertThrows(ErreurMetierException.class, () -> {
            service.creerRendezVous(request4);
        });

        assertEquals(409, exception.getStatusCode());
        assertEquals("Ce créneau est complet.", exception.getMessage());
    }

    @Test
    void testTMA103_ValiderNPIMalforme() {
        CreerRendezVousRequest request = new CreerRendezVousRequest(
            "06142A3647",
            "CNI",
            "2026-11-23T09:00"
        );

        ErreurMetierException exception = assertThrows(ErreurMetierException.class, () -> {
            service.creerRendezVous(request);
        });

        assertEquals(400, exception.getStatusCode());
        assertEquals("Le NPI doit comporter exactement 10 chiffres.", exception.getMessage());
    }

    @Test
    void testTMA104_AnnulationPlusDe24HeuresAvant() {
        LocalDateTime dateRdv = LocalDateTime.of(2026, 11, 19, 10, 0);
        LocalDateTime dateAnnulation = LocalDateTime.of(2026, 11, 17, 18, 0);

        service.setMaintenant(dateAnnulation);

        RendezVous rdv = new RendezVous(709L, "0203040506", ServiceType.PASSEPORT, dateRdv, Statut.CONFIRME, dateAnnulation.minusDays(1));
        service.ajouterRendezVous(rdv);

        service.annulerRendezVous(709L);

        RendezVousResponse rdvModifie = service.creerRendezVous(new CreerRendezVousRequest("9999999999", "CNI", "2026-12-01T10:00"));
        assertNotNull(rdvModifie);
    }

    @Test
    void testTMA105_RefuserChangementStatutDefinitif() {
        RendezVous rdv = new RendezVous(292L, "0330677555", ServiceType.CNI, LocalDateTime.of(2026, 11, 15, 10, 0), Statut.ANNULE, LocalDateTime.now());
        service.ajouterRendezVous(rdv);

        ErreurMetierException exception = assertThrows(ErreurMetierException.class, () -> {
            service.changerStatut(292L, "honore");
        });

        assertEquals(409, exception.getStatusCode());
        assertEquals("Ce rendez-vous est déjà clôturé.", exception.getMessage());
    }

    @Test
    void testTMA106_TrierRendezVousParDateDecroissante() {
        service.setMaintenant(LocalDateTime.of(2026, 10, 1, 0, 0));

        RendezVous rdv1 = new RendezVous(1L, "0203040506", ServiceType.PASSEPORT, LocalDateTime.of(2026, 12, 2, 10, 0), Statut.CONFIRME, LocalDateTime.now());
        RendezVous rdv2 = new RendezVous(2L, "0203040506", ServiceType.CNI, LocalDateTime.of(2026, 11, 15, 10, 0), Statut.CONFIRME, LocalDateTime.now());
        RendezVous rdv3 = new RendezVous(3L, "0203040506", ServiceType.PERMIS, LocalDateTime.of(2026, 10, 20, 10, 0), Statut.HONORE, LocalDateTime.now());

        service.ajouterRendezVous(rdv1);
        service.ajouterRendezVous(rdv2);
        service.ajouterRendezVous(rdv3);

        PageRendezVousResponse response = service.listerRendezVousUsager("0203040506", 1);

        assertEquals(3, response.total());
        assertTrue(response.rendezVous().get(0).dateHeure().startsWith("02/12/2026"));
    }

    @Test
    void testTMA107_RefuserRendezVousQuandDejaUnAVenir() {
        service.setMaintenant(LocalDateTime.of(2026, 11, 10, 0, 0));

        RendezVous rdvExistant = new RendezVous(100L, "0102030405", ServiceType.PASSEPORT, LocalDateTime.of(2026, 11, 27, 9, 0), Statut.CONFIRME, LocalDateTime.now());
        service.ajouterRendezVous(rdvExistant);

        CreerRendezVousRequest request = new CreerRendezVousRequest(
            "0102030405",
            "PERMIS",
            "2026-11-27T09:00"
        );

        ErreurMetierException exception = assertThrows(ErreurMetierException.class, () -> {
            service.creerRendezVous(request);
        });

        assertEquals(409, exception.getStatusCode());
        assertEquals("Vous avez déjà un rendez-vous à venir.", exception.getMessage());
    }

    @Test
    void testAnnulationMoinsDe24HeuresAvant() {
        LocalDateTime dateRdv = LocalDateTime.of(2026, 11, 19, 10, 0);
        LocalDateTime dateAnnulation = LocalDateTime.of(2026, 11, 18, 15, 0);

        service.setMaintenant(dateAnnulation);

        RendezVous rdv = new RendezVous(709L, "0203040506", ServiceType.PASSEPORT, dateRdv, Statut.CONFIRME, dateAnnulation.minusDays(1));
        service.ajouterRendezVous(rdv);

        service.annulerRendezVous(709L);

        ErreurMetierException exception = assertThrows(ErreurMetierException.class, () -> {
            service.creerRendezVous(new CreerRendezVousRequest("0203040506", "CNI", "2026-12-01T10:00"));
        });

        assertEquals(409, exception.getStatusCode());
        assertEquals("Annulation impossible moins de 24 h avant le rendez-vous.", exception.getMessage());
    }

    @Test
    void testChangerStatutHonore() {
        RendezVous rdv = new RendezVous(292L, "0330677555", ServiceType.CNI, LocalDateTime.of(2026, 11, 15, 10, 0), Statut.CONFIRME, LocalDateTime.now());
        service.ajouterRendezVous(rdv);

        RendezVousResponse response = service.changerStatut(292L, "honore");

        assertNotNull(response);
        assertEquals("honore", response.statut());
    }

    @Test
    void testChangerStatutAbsent() {
        RendezVous rdv = new RendezVous(292L, "0330677555", ServiceType.CNI, LocalDateTime.of(2026, 11, 15, 10, 0), Statut.CONFIRME, LocalDateTime.now());
        service.ajouterRendezVous(rdv);

        RendezVousResponse response = service.changerStatut(292L, "absent");

        assertNotNull(response);
        assertEquals("absent", response.statut());
    }

    @Test
    void testCreneauInvalideHeure() {
        CreerRendezVousRequest request = new CreerRendezVousRequest(
            "0123456789",
            "CNI",
            "2026-11-23T07:00"
        );

        ErreurMetierException exception = assertThrows(ErreurMetierException.class, () -> {
            service.creerRendezVous(request);
        });

        assertEquals(409, exception.getStatusCode());
        assertEquals("Créneau invalide : de 08:00 à 15:30, toutes les 30 minutes.", exception.getMessage());
    }

    @Test
    void testDatePassee() {
        CreerRendezVousRequest request = new CreerRendezVousRequest(
            "0123456789",
            "CNI",
            "2026-11-03T10:00"
        );

        ErreurMetierException exception = assertThrows(ErreurMetierException.class, () -> {
            service.creerRendezVous(request);
        });

        assertEquals(409, exception.getStatusCode());
        assertEquals("Impossible de prendre un rendez-vous dans le passé.", exception.getMessage());
    }

    @Test
    void testServiceInconnu() {
        CreerRendezVousRequest request = new CreerRendezVousRequest(
            "0123456789",
            "UNKNOWN",
            "2026-11-23T10:00"
        );

        ErreurMetierException exception = assertThrows(ErreurMetierException.class, () -> {
            service.creerRendezVous(request);
        });

        assertEquals(400, exception.getStatusCode());
        assertEquals("Service inconnu : choisissez PASSEPORT, CNI ou PERMIS.", exception.getMessage());
    }

    @Test
    void testDateHeureIllisible() {
        CreerRendezVousRequest request = new CreerRendezVousRequest(
            "0123456789",
            "CNI",
            "invalid-date"
        );

        ErreurMetierException exception = assertThrows(ErreurMetierException.class, () -> {
            service.creerRendezVous(request);
        });

        assertEquals(400, exception.getStatusCode());
        assertEquals("Date et heure invalides (format attendu : AAAA-MM-JJTHH:MM).", exception.getMessage());
    }
}
