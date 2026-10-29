package etude_cas_asin.asin_test.model;

import java.time.LocalDateTime;

public class RendezVous {
    private Long id;
    private String npi;
    private ServiceType service;
    private LocalDateTime dateHeure;
    private Statut statut;
    private LocalDateTime creeLe;

    public RendezVous() {}

    public RendezVous(Long id, String npi, ServiceType service, LocalDateTime dateHeure, Statut statut, LocalDateTime creeLe) {
        this.id = id;
        this.npi = npi;
        this.service = service;
        this.dateHeure = dateHeure;
        this.statut = statut;
        this.creeLe = creeLe;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNpi() {
        return npi;
    }

    public void setNpi(String npi) {
        this.npi = npi;
    }

    public ServiceType getService() {
        return service;
    }

    public void setService(ServiceType service) {
        this.service = service;
    }

    public LocalDateTime getDateHeure() {
        return dateHeure;
    }

    public void setDateHeure(LocalDateTime dateHeure) {
        this.dateHeure = dateHeure;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public LocalDateTime getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(LocalDateTime creeLe) {
        this.creeLe = creeLe;
    }

    public boolean estAVenir(LocalDateTime maintenant) {
        return dateHeure.isAfter(maintenant);
    }

    public boolean estPasse(LocalDateTime maintenant) {
        return dateHeure.isBefore(maintenant);
    }
}
