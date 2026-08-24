package dev.notzyvex.coasters_extras.cart;

public enum CartPart {

    BODY("body", "Cart Body"),

    BOGIE("bogie", "Wheel Bogie"),

    NOSE("nose", "Cart Nose"),

    SEAT("seat", "Cart Seat"),

    RESTRAINT("restraint", "Lap Restraint"),

    CAMERA("camera", "Ride Camera");

    private final String id;
    private final String display;

    CartPart(String id, String display) {
        this.id = id;
        this.display = display;
    }

    public String id() {
        return id;
    }

    public String display() {
        return display;
    }
}
