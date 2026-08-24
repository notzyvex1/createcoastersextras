package dev.notzyvex.coasters_extras.cart;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record CartPrefab(String id, String display, List<Placement> parts) {

    public record Placement(CartPart part, BlockPos offset) { }

    private static Placement at(CartPart part, int x, int y, int z) {
        return new Placement(part, new BlockPos(x, y, z));
    }

    private static CartPrefab straight(String id, String display, int segments, boolean restraint) {
        List<Placement> parts = new ArrayList<>();
        for (int x = 0; x < segments; x++) {
            parts.add(at(CartPart.BODY, x, 0, 0));
            parts.add(at(CartPart.SEAT, x, 1, 0));
            if (restraint) {
                parts.add(at(CartPart.RESTRAINT, x, 2, 0));
            }
        }
        parts.add(at(CartPart.NOSE, segments, 0, 0));
        parts.add(at(CartPart.BOGIE, 0, -1, 0));
        if (segments > 1) {
            parts.add(at(CartPart.BOGIE, segments - 1, -1, 0));
        }
        return new CartPrefab(id, display, List.copyOf(parts));
    }

    public static final List<CartPrefab> ALL = List.of(
            new CartPrefab("compact", "Compact Cart", List.of(
                    at(CartPart.BODY, 0, 0, 0),
                    at(CartPart.SEAT, 0, 1, 0),
                    at(CartPart.RESTRAINT, 0, 2, 0),
                    at(CartPart.BOGIE, 0, -1, 0))),
            straight("standard", "Standard Cart", 2, true),
            straight("large", "Large Cart", 3, true),
            new CartPrefab("long", "Long Cart", buildLong()),
            straight("modern", "Modern Cart", 2, true),
            straight("classic", "Classic Cart", 2, false),
            straight("industrial", "Industrial Cart", 2, true),
            new CartPrefab("open_wheel", "Open-Wheel Cart", List.of(
                    at(CartPart.SEAT, 0, 0, 0),
                    at(CartPart.SEAT, 1, 0, 0),
                    at(CartPart.RESTRAINT, 0, 1, 0),
                    at(CartPart.RESTRAINT, 1, 1, 0),
                    at(CartPart.BOGIE, 0, -1, 0),
                    at(CartPart.BOGIE, 1, -1, 0))));

    private static List<Placement> buildLong() {
        List<Placement> parts = new ArrayList<>();
        for (int x = 0; x < 4; x++) {
            parts.add(at(CartPart.BODY, x, 0, 0));
            parts.add(at(CartPart.SEAT, x, 1, 0));
            parts.add(at(CartPart.RESTRAINT, x, 2, 0));
        }
        parts.add(at(CartPart.NOSE, 4, 0, 0));
        for (int x : new int[]{0, 1, 2, 3}) {
            parts.add(at(CartPart.BOGIE, x, -1, 0));
        }
        return List.copyOf(parts);
    }
}
