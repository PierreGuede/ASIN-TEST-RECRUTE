package etude_cas_asin.asin_test.model;

public enum Statut {
    CONFIRME("confirme"),
    HONORE("honore"),
    ABSENT("absent"),
    ANNULE("annule");

    private final String value;

    Statut(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Statut fromString(String value) {
        if (value == null) {
            return null;
        }
        for (Statut statut : Statut.values()) {
            if (statut.value.equalsIgnoreCase(value)) {
                return statut;
            }
        }
        return null;
    }

    public boolean isDefinitif() {
        return this == HONORE || this == ABSENT || this == ANNULE;
    }
}
