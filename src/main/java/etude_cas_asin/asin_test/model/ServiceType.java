package etude_cas_asin.asin_test.model;

public enum ServiceType {
    PASSEPORT,
    CNI,
    PERMIS;

    public static ServiceType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ServiceType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
