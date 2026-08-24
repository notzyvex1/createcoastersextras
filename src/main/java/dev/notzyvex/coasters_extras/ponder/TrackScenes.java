package dev.notzyvex.coasters_extras.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.notzyvex.coasters_extras.sensor.SensorBlock;
import dev.notzyvex.coasters_extras.track.ModTracks;
import dev.silvergold.simulatedcoasters.ponder.CoasterCartPonderExtras;
import dev.silvergold.simulatedcoasters.ponder.elements.CoasterCartExtrasElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class TrackScenes {

    private static final BlockPos ANCHOR_IN = new BlockPos(3, 1, 7);
    private static final BlockPos ANCHOR_OUT = new BlockPos(11, 1, 7);
    private static final BlockPos FAKE_TRACK = new BlockPos(3, 2, 7);
    private static final BlockPos MIDDLE = new BlockPos(7, 2, 7);
    private static final BlockPos CART = new BlockPos(5, 2, 7);
    private static final BlockPos SENSOR_BLOCK = new BlockPos(7, 1, 5);
    private static final BlockPos LAMP = new BlockPos(7, 1, 4);

    private static final float CART_YAW = 90.0F;

    public static void boost(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = open(builder, util, "boost_track", "Making a coaster move");

        scene.overlay().showOutlineWithText(track(util), 100)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.WHITE)
                .text("Plain track carries a coaster. It never pushes one.");
        scene.idle(110);

        ElementLink<CoasterCartExtrasElement> cart = showCart(scene, util);
        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.SLOW)
                .text("So it slows down, and stops.");
        scene.idle(110);

        scene.overlay().showOutlineWithText(track(util), 100)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.GREEN)
                .text("Boost Track pushes it instead.");
        scene.idle(110);

        scene.effects().indicateSuccess(MIDDLE);
        scene.idle(20);

        scene.overlay().showText(110)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.FAST)
                .text("This is how you make a coaster move.");
        scene.idle(120);

        scene.overlay().showControls(util.vector().topOf(ANCHOR_OUT), Pointing.DOWN, 60)
                .withItem(new ItemStack(ModTracks.BOOST_TRACK.get()))
                .scroll();
        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_OUT))
                .colored(PonderPalette.INPUT)
                .text("Scroll an anchorpoint to set the speed.");
        scene.idle(110);

        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_IN))
                .colored(PonderPalette.BLUE)
                .text("Either end works. They stay in sync.");
        scene.idle(110);

        CoasterCartPonderExtras.hide(scene, cart, Direction.UP);
        scene.markAsFinished();
    }

    public static void splash(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = open(builder, util, "splash_track", "Running through water");

        ElementLink<CoasterCartExtrasElement> cart = showCart(scene, util);
        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.BLUE)
                .text("Splash Track throws water off both sides as a coaster passes.");
        scene.idle(110);

        scene.effects().indicateSuccess(MIDDLE);
        scene.idle(20);

        scene.overlay().showOutlineWithText(track(util), 100)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.SLOW)
                .text("Water drags. On its own, a long splash run strands a ride in it.");
        scene.idle(110);

        scene.overlay().showControls(util.vector().topOf(ANCHOR_OUT), Pointing.DOWN, 60)
                .withItem(new ItemStack(ModTracks.SPLASH_TRACK.get()))
                .scroll();
        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_OUT))
                .colored(PonderPalette.INPUT)
                .text("Dial a Water Boost and the section drives the ride instead.");
        scene.idle(110);

        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.GREEN)
                .text("Left at zero it only slows, which is the old behaviour.");
        scene.idle(110);

        CoasterCartPonderExtras.hide(scene, cart, Direction.UP);
        scene.markAsFinished();
    }

    public static void launch(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = open(builder, util, "launch_track", "Launching a coaster");

        scene.overlay().showOutlineWithText(track(util), 100)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.WHITE)
                .text("A Boost Track eases a coaster up to speed.");
        scene.idle(110);

        ElementLink<CoasterCartExtrasElement> cart = showCart(scene, util);
        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.FAST)
                .text("Launch Track throws it. Standstill to full speed at once.");
        scene.idle(110);

        scene.effects().indicateSuccess(MIDDLE);
        scene.idle(20);

        scene.overlay().showText(110)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.RED)
                .text("It will start a coaster that has stopped dead.");
        scene.idle(120);

        scene.overlay().showControls(util.vector().topOf(ANCHOR_OUT), Pointing.DOWN, 60)
                .withItem(new ItemStack(ModTracks.LAUNCH_TRACK.get()))
                .scroll();
        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_OUT))
                .colored(PonderPalette.INPUT)
                .text("Scroll an anchorpoint to set the launch speed.");
        scene.idle(110);

        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_IN))
                .colored(PonderPalette.BLUE)
                .text("Send picks which way it fires. Forward, or reverse.");
        scene.idle(110);

        CoasterCartPonderExtras.hide(scene, cart, Direction.UP);
        scene.markAsFinished();
    }

    public static void bobsled(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = open(builder, util, "bobsled_track", "Riding without rails");

        scene.overlay().showOutlineWithText(track(util), 100)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.WHITE)
                .text("Coaster track bolts a cart to the rail. It cannot lean.");
        scene.idle(110);

        ElementLink<CoasterCartExtrasElement> cart = showCart(scene, util);
        scene.overlay().showText(110)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.BLUE)
                .text("Bobsled Track lets go of the roll.");
        scene.idle(120);

        scene.effects().indicateSuccess(MIDDLE);
        scene.idle(20);

        scene.overlay().showText(120)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.FAST)
                .text("Carry speed into a turn and it banks up the wall on its own.");
        scene.idle(130);

        scene.overlay().showText(110)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_OUT))
                .colored(PonderPalette.GREEN)
                .text("Nothing to set. How hard it leans is how fast you took the corner.");
        scene.idle(120);

        CoasterCartPonderExtras.hide(scene, cart, Direction.UP);
        scene.markAsFinished();
    }

    public static void powered(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = open(builder, util, "powered", "Running a track on redstone");

        scene.overlay().showOutlineWithText(track(util), 100)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.WHITE)
                .text("Boost, Brake and Launch all work the moment a coaster touches them.");
        scene.idle(110);

        scene.overlay().showControls(util.vector().topOf(ANCHOR_OUT), Pointing.DOWN, 60)
                .withItem(new ItemStack(ModTracks.BOOST_TRACK.get()))
                .scroll();
        scene.overlay().showText(110)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_OUT))
                .colored(PonderPalette.INPUT)
                .text("The Powered dial on the anchorpoint changes that.");
        scene.idle(120);

        scene.overlay().showText(110)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_OUT))
                .colored(PonderPalette.BLUE)
                .text("Set it to On Redstone and the track waits for a signal.");
        scene.idle(120);

        ElementLink<CoasterCartExtrasElement> cart = showCart(scene, util);
        scene.overlay().showText(110)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.RED)
                .text("Unpowered it does nothing at all. The coaster just rolls over it.");
        scene.idle(120);

        scene.world().toggleRedstonePower(util.select().position(ANCHOR_OUT));
        scene.effects().indicateRedstone(ANCHOR_OUT);
        scene.idle(20);

        scene.overlay().showText(110)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.FAST)
                .text("Power the anchorpoint and it works again.");
        scene.idle(120);

        scene.effects().indicateSuccess(MIDDLE);
        scene.idle(20);

        scene.overlay().showText(120)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_IN))
                .colored(PonderPalette.GREEN)
                .text("Launch on a button. Dispatch on a timer. Hold a section shut.");
        scene.idle(130);

        CoasterCartPonderExtras.hide(scene, cart, Direction.UP);
        scene.markAsFinished();
    }

    public static void reverse(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = open(builder, util, "reverse_track",
                "Sending a coaster back");

        ElementLink<CoasterCartExtrasElement> cart = showCart(scene, util);
        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.WHITE)
                .text("A coaster runs on until the track runs out.");
        scene.idle(110);

        scene.overlay().showOutlineWithText(track(util), 100)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.MEDIUM)
                .text("Reverse Track turns it around instead.");
        scene.idle(110);

        scene.effects().indicateSuccess(MIDDLE);
        scene.idle(20);

        scene.overlay().showText(110)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.GREEN)
                .text("Once per pass, so it leaves rather than rocking in place.");
        scene.idle(120);

        scene.overlay().showControls(util.vector().topOf(ANCHOR_OUT), Pointing.DOWN, 60)
                .withItem(new ItemStack(ModTracks.REVERSE_TRACK.get()))
                .scroll();
        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_OUT))
                .colored(PonderPalette.INPUT)
                .text("Reverse Boost sets how fast it sends the ride back out.");
        scene.idle(110);

        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_IN))
                .colored(PonderPalette.BLUE)
                .text("Set Send and it forces one direction instead of flipping.");
        scene.idle(110);

        CoasterCartPonderExtras.hide(scene, cart, Direction.UP);
        scene.markAsFinished();
    }

    public static void brake(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = open(builder, util, "brake_track", "Slowing a coaster down");

        ElementLink<CoasterCartExtrasElement> cart = showCart(scene, util);
        scene.overlay().showText(90)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.FAST)
                .text("A coaster arrives fast.");
        scene.idle(100);

        scene.overlay().showOutlineWithText(track(util), 100)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.RED)
                .text("Brake Track slows it to the speed you set.");
        scene.idle(110);

        scene.overlay().showText(120)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_OUT))
                .colored(PonderPalette.WHITE)
                .text("It brakes by how much track is left, so even a fast one stops in time.");
        scene.idle(130);

        scene.overlay().showControls(util.vector().topOf(ANCHOR_OUT), Pointing.DOWN, 60)
                .withItem(new ItemStack(ModTracks.BRAKE_TRACK.get()))
                .scroll();
        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_OUT))
                .colored(PonderPalette.INPUT)
                .text("Set it to zero and it stops dead.");
        scene.idle(110);

        CoasterCartPonderExtras.hide(scene, cart, Direction.UP);
        scene.markAsFinished();
    }

    public static void station(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = open(builder, util, "station_track", "Stopping and dispatching");

        ElementLink<CoasterCartExtrasElement> cart = showCart(scene, util);
        scene.overlay().showOutlineWithText(track(util), 100)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.MEDIUM)
                .text("Station Track brings a coaster in gently.");
        scene.idle(110);

        scene.overlay().showOutlineWithText(util.select().position(ANCHOR_OUT), 110)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.GREEN)
                .text("It stops at the last anchorpoint, every time.");
        scene.idle(120);

        scene.overlay().showText(110)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.RED)
                .text("The whole train stops together.");
        scene.idle(120);

        scene.overlay().showText(110)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_IN))
                .colored(PonderPalette.INPUT)
                .text("Then it waits. Scroll to set how long.");
        scene.idleSeconds(6);

        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_OUT))
                .colored(PonderPalette.WHITE)
                .text("Goggles show the countdown.");
        scene.idle(110);

        scene.overlay().showText(90)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.OUTPUT)
                .text("Then it sets off again on its own.");
        scene.idle(30);

        CoasterCartPonderExtras.hide(scene, cart, Direction.EAST);
        scene.idle(70);

        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_IN))
                .colored(PonderPalette.RED)
                .text("Redstone holds it in the station.");
        scene.idle(110);

        scene.markAsFinished();
    }

    public static void sensor(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = open(builder, util, "sensor_track", "Spotting a coaster");

        scene.overlay().showOutlineWithText(track(util), 100)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.WHITE)
                .text("Sensor Track spots every coaster that crosses it.");
        scene.idle(110);

        scene.world().showSection(util.select().fromTo(SENSOR_BLOCK, LAMP), Direction.DOWN);
        scene.overlay().showOutlineWithText(util.select().position(SENSOR_BLOCK), 110)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.INPUT)
                .text("Right-click it with a Sensor Block to link them.");
        scene.idle(120);

        ElementLink<CoasterCartExtrasElement> cart = showCart(scene, util);

        scene.effects().indicateSuccess(MIDDLE);
        scene.world().modifyBlock(SENSOR_BLOCK,
                st -> st.setValue(SensorBlock.POWERED, true), false);
        scene.world().toggleRedstonePower(util.select().position(LAMP));
        scene.effects().indicateRedstone(LAMP);
        scene.idle(10);

        scene.overlay().showText(110)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(LAMP))
                .colored(PonderPalette.RED)
                .text("Now the block gives redstone when a coaster passes.");
        scene.idle(120);

        scene.overlay().showText(120)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(SENSOR_BLOCK))
                .colored(PonderPalette.WHITE)
                .text("The block can go anywhere. Put it by your wiring.");
        scene.idle(130);

        CoasterCartPonderExtras.hide(scene, cart, Direction.UP);
        scene.markAsFinished();
    }

    public static void slippery(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = open(builder, util, "slippery_track", "Keeping the speed");

        scene.overlay().showOutlineWithText(track(util), 100)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.BLUE)
                .text("Coasters normally lose speed to drag.");
        scene.idle(110);

        ElementLink<CoasterCartExtrasElement> cart = showCart(scene, util);
        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.FAST)
                .text("Slippery Track cancels it.");
        scene.idle(110);

        scene.overlay().showText(120)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(ANCHOR_OUT))
                .colored(PonderPalette.WHITE)
                .text("Good for long flat runs between hills.");
        scene.idle(130);

        CoasterCartPonderExtras.hide(scene, cart, Direction.UP);
        scene.markAsFinished();
    }

    public static void rainbow(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = open(builder, util, "rainbow_track", "Rainbow Coaster Track");

        scene.overlay().showOutlineWithText(track(util), 100)
                .attachKeyFrame().placeNearTarget().colored(PonderPalette.OUTPUT)
                .text("Rainbow Track sweeps colour along its length.");
        scene.idle(110);

        scene.rotateCameraY(90);
        scene.idle(30);

        ElementLink<CoasterCartExtrasElement> cart = showCart(scene, util);
        scene.overlay().showText(100)
                .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(MIDDLE))
                .colored(PonderPalette.WHITE)
                .text("Purely decorative. Rides like plain track.");
        scene.idle(110);

        CoasterCartPonderExtras.hide(scene, cart, Direction.UP);
        scene.markAsFinished();
    }

    private static net.createmod.ponder.api.scene.Selection track(SceneBuildingUtil util) {
        return util.select().fromTo(3, 1, 7, 11, 1, 7);
    }

    private static CreateSceneBuilder open(SceneBuilder builder, SceneBuildingUtil util,
                                           String id, String title) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(id, title);
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.75F);
        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().layer(1), Direction.DOWN);
        scene.world().showSection(util.select().position(FAKE_TRACK), Direction.DOWN);
        scene.idle(20);
        return scene;
    }

    private static ElementLink<CoasterCartExtrasElement> showCart(CreateSceneBuilder scene,
                                                             SceneBuildingUtil util) {
        ElementLink<CoasterCartExtrasElement> cart = CoasterCartPonderExtras.show(
                scene, CoasterCartExtrasElement.carts(CART_YAW, CART), Direction.DOWN);
        scene.idle(20);
        return cart;
    }
}
