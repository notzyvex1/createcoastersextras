package dev.notzyvex.coasters_extras.track;

import dev.notzyvex.coasters_extras.CreateTooltip;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The item behind every functional track.
 *
 * <p>Extends the base mod's {@link CoasterTrackItem} so we inherit their entire placement
 * mechanic -- anchorpoints, connections, chain lifts, the lot -- rather than reimplementing
 * it. Their item is an {@code Item}, not a {@code BlockItem}: coaster track is not placed
 * block by block, it is laid between anchorpoints by this item's {@code useOn}.
 *
 * <p>One class serves all five functional tracks, so the tooltip is chosen from the item's own
 * registry name. Adding a track means adding a case, and nothing else.
 */
public class BoostTrackItem extends CoasterTrackItem {

    public BoostTrackItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * The track material this item lays.
     *
     * <p>Resolved from the registry name for the same reason the tooltip below is: one class
     * serves all five functional tracks, so there is no field to read. Keeping the mapping
     * here rather than at the call site means a new functional track is still one case in one
     * file.
     *
     * <p>Used to convert a curve that is already in the world -- a functional track IS its
     * material, so swapping the material of a placed curve is the whole of "make this section
     * a brake" without breaking and relaying it.
     */
    public com.simibubi.create.content.trains.track.TrackMaterial material() {
        return switch (BuiltInRegistries.ITEM.getKey(this).getPath()) {
            case "boost_track"    -> ModTrackMaterials.BOOST;
            case "brake_track"    -> ModTrackMaterials.BRAKE;
            case "station_track"  -> ModTrackMaterials.STATION;
            case "sensor_track"   -> ModTrackMaterials.SENSOR;
            case "slippery_track" -> ModTrackMaterials.SLIPPERY;
            case "bobsled_track"  -> ModTrackMaterials.BOBSLED;
            case "splash_track"   -> ModTrackMaterials.SPLASH;
            case "launch_track"   -> ModTrackMaterials.LAUNCH;
            case "reverse_track"  -> ModTrackMaterials.REVERSE;
            case "powered_boost_track" -> ModTrackMaterials.POWERED_BOOST;
            default -> null;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        if (CreateTooltip.collapsed(tooltip)) {
            return;
        }

        switch (BuiltInRegistries.ITEM.getKey(this).getPath()) {
            case "boost_track" -> {
                CreateTooltip.title(tooltip, "Boost Track", ChatFormatting.GOLD);
                CreateTooltip.summary(tooltip,
                        "_Drives_ a coaster forward. This is how a ride moves without a _chain lift_");
                CreateTooltip.pair(tooltip, "While a Coaster is on it",
                        "_Accelerates_ it up to the speed set on the anchorpoints");
                CreateTooltip.pair(tooltip, "When Scrolled on an Anchorpoint",
                        "Sets the _target speed_. Both ends stay in _sync_");
                CreateTooltip.pair(tooltip, "Direction",
                        "Never _reverses_ a coaster. It pushes the way it is already going");
            }
            case "brake_track" -> {
                CreateTooltip.title(tooltip, "Brake Track", ChatFormatting.RED);
                CreateTooltip.summary(tooltip,
                        "_Slows_ a coaster to the speed you choose, or stops it _dead_");
                CreateTooltip.pair(tooltip, "While a Coaster is on it",
                        "Brakes by the _track remaining_, not at a fixed rate, so even full "
                      + "speed is down to target by the _end of the run_");
                CreateTooltip.pair(tooltip, "When Set to Zero",
                        "Brings it to a _complete stop_");
            }
            case "station_track" -> {
                CreateTooltip.title(tooltip, "Station Track", ChatFormatting.AQUA);
                CreateTooltip.summary(tooltip,
                        "Brings a ride in, _holds_ it, and _dispatches_ it again");
                CreateTooltip.pair(tooltip, "When a Coaster Arrives",
                        "Eases it to a halt at the _last anchorpoint_, however fast it came in. "
                      + "The _whole train_ freezes together");
                CreateTooltip.pair(tooltip, "When Scrolled on an Anchorpoint",
                        "Sets how many _seconds_ it waits before dispatching");
                CreateTooltip.pair(tooltip, "While Powered by Redstone",
                        "_Holds_ the coaster in the station");
                CreateTooltip.pair(tooltip, "With Engineer's Goggles",
                        "Shows a live _countdown_ on the anchorpoint");
            }
            case "sensor_track" -> {
                CreateTooltip.title(tooltip, "Sensor Track", ChatFormatting.YELLOW);
                CreateTooltip.summary(tooltip,
                        "_Notices_ every coaster that crosses it");
                CreateTooltip.pair(tooltip, "When a Coaster Passes",
                        "_Sparks_, marking the moment on the ride");
                CreateTooltip.pair(tooltip, "With a Linked Sensor Block",
                        "That block emits a _redstone signal_. It can sit _anywhere_, not just "
                      + "beside the track");
            }
            case "slippery_track" -> {
                CreateTooltip.title(tooltip, "Slippery Track", ChatFormatting.BLUE);
                CreateTooltip.summary(tooltip,
                        "Cancels the _drag_ a coaster normally loses speed to");
                CreateTooltip.pair(tooltip, "While a Coaster is on it",
                        "Comes out as fast as it went _in_");
                CreateTooltip.pair(tooltip, "Best Used For",
                        "_Long flat runs_ between hills, where a ride would otherwise stall "
                      + "short of the station");
            }
            case "bobsled_track" -> {
                CreateTooltip.title(tooltip, "Bobsled Track", ChatFormatting.AQUA);
                CreateTooltip.summary(tooltip,
                        "The cart rides _free in a trough_ instead of bolted to the rail");
                CreateTooltip.pair(tooltip, "While a Coaster is on it",
                        "_Slides_ across the channel and _banks_ up the walls with its own speed");
                CreateTooltip.pair(tooltip, "Best Used For",
                        "_Sweeping banked turns_ that a rigid rail cannot feel");
            }
            case "splash_track" -> {
                CreateTooltip.title(tooltip, "Splash Track", ChatFormatting.AQUA);
                CreateTooltip.summary(tooltip,
                        "Throws a big _splash_ off both sides as a coaster tears through");
                CreateTooltip.pair(tooltip, "While a Coaster is on it",
                        "Erupts a splash on _both sides_ with a splash sound, bigger the "
                      + "_faster_ it hits");
                CreateTooltip.pair(tooltip, "Effect on the Ride",
                        "A little _water drag_. Slows it a touch, never stops it");
                CreateTooltip.pair(tooltip, "Best Used For",
                        "_Water sections_ and log-flume style splashdowns");
            }
            case "launch_track" -> {
                CreateTooltip.title(tooltip, "Launch Track", ChatFormatting.GOLD);
                CreateTooltip.summary(tooltip,
                        "A _hydraulic launch_ -- flings a ride from a standstill to _high speed_");
                CreateTooltip.pair(tooltip, "While a Coaster is on it",
                        "Kicks it _hard_ up to the launch speed, far stronger than a _boost_, "
                      + "with fire and smoke");
                CreateTooltip.pair(tooltip, "When Scrolled on an Anchorpoint",
                        "Sets the _launch speed_ and the _direction_ (Send), both ends in _sync_");
                CreateTooltip.pair(tooltip, "Direction",
                        "_Will_ start a stopped coaster -- set Send to force which way it launches");
            }
            case "reverse_track" -> {
                CreateTooltip.title(tooltip, "Reverse Track", ChatFormatting.LIGHT_PURPLE);
                CreateTooltip.summary(tooltip,
                        "_Flips_ a coaster's direction as it crosses -- turns any dead-end into "
                      + "a _shuttle_");
                CreateTooltip.pair(tooltip, "When a Coaster Passes",
                        "Reverses its travel direction _once per pass_, so it does not oscillate");
                CreateTooltip.pair(tooltip, "Best Used For",
                        "_Boomerang_ and shuttle layouts that run out and back");
            }
            case "powered_boost_track" -> {
                CreateTooltip.title(tooltip, "Powered Boost Track", ChatFormatting.RED);
                CreateTooltip.summary(tooltip,
                        "A boost that only pushes while it has _redstone_");
                CreateTooltip.pair(tooltip, "While Powered",
                        "_Accelerates_ a coaster exactly like a Boost Track");
                CreateTooltip.pair(tooltip, "While Unpowered",
                        "Does _nothing_. A ride coasts across it untouched");
                CreateTooltip.pair(tooltip, "Best Used For",
                        "_Launch buttons_, timed dispatches, and sections you want to hold shut");
            }
            default -> {
                CreateTooltip.title(tooltip, "Coaster Track", ChatFormatting.WHITE);
                CreateTooltip.summary(tooltip, "Cosmetic. Rides exactly like _plain track_");
            }
        }
    }
}
