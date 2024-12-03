package com.tramchester.domain.reference;

public enum GTFSPickupDropoffType {
    Regular, // OR empty string
    None,
    Phone,
    Driver;

    public static GTFSPickupDropoffType fromString(final String text) {
        if (text.isEmpty()) {
            return Regular;
        }
        return switch (text) {
            case "0" -> Regular;
            case "1" -> None;
            case "2" -> Phone;
            case "3" -> Driver;
            default -> throw new RuntimeException("Unrecognised '" + text + "'");
        };
    }

    public boolean isPickup() {
        return this!=None;
    }

    public boolean isDropOff() {
        return this!=None;
    }
}
