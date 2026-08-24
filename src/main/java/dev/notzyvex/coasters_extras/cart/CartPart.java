package dev.notzyvex.coasters_extras.cart;

/**
 * The pieces a cart chassis is built from.
 *
 * <p><b>Parts, not whole carts.</b> Eight complete cart models would be eight modelling jobs and
 * eight things to restyle later. Five parts combined in different layouts gives the same eight
 * carts -- and a Long Cart is then literally "more body segments and more bogies", which is what a
 * long cart IS in the real world.
 *
 * <p>These are ordinary blocks. They are placed inside the cart's Sable sublevel around the base
 * mod's own {@code COASTER_CART} block, which stays exactly where it is -- so every one of the
 * twenty-four places that compare against that block by identity still sees it, and nothing has to
 * be mixed into.
 */
public enum CartPart {

    /** The floor and sides. Repeated front-to-back to make a cart longer. */
    BODY("body", "Cart Body"),

    /** A pair of wheels on the rail. More of these is what makes a big cart look heavy. */
    BOGIE("bogie", "Wheel Bogie"),

    /** The shaped front. One per cart, at the leading end. */
    NOSE("nose", "Cart Nose"),

    /** Where a rider sits. Purely visual -- riding is handled by the sublevel itself. */
    SEAT("seat", "Cart Seat"),

    /** The bar that comes down over the rider. The detail that reads as "coaster". */
    RESTRAINT("restraint", "Lap Restraint"),

    /**
     * An on-ride camera rig, for filming a POV.
     *
     * <p>Purely a prop -- {@code CameraCartEntity} does the actual filming, and it deliberately
     * follows the track SPLINE rather than a cart, because a camera bolted to a cart inherits
     * every jolt the cart takes. What was missing was that a ride being filmed had nothing on it
     * to show for it, so footage of a coaster and footage of a coaster being filmed looked
     * identical. This is that missing visual.
     *
     * <p>Built out of cubes on existing Create and vanilla textures, like every other part here:
     * a part that needs a bespoke texture sheet is a part that does not get made.
     */
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
