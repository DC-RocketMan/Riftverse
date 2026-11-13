package dev.riftverse.tech.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;

/** Kleine Helfer, die alle Riftverse-Menüs teilen. */
public final class MenuHelper {
    public static final int INV_X = 8, INV_Y = 84, HOTBAR_Y = 142;

    /** Fügt Spielerinventar und Hotbar hinzu (27 + 9 Slots). */
    public static void addPlayerInventory(Inventory inventory, java.util.function.Consumer<Slot> add) {
        addPlayerInventory(inventory, add, 0);
    }

    /** Wie oben, für höhere GUIs um {@code dy} Pixel nach unten verschoben. */
    public static void addPlayerInventory(Inventory inventory, java.util.function.Consumer<Slot> add, int dy) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                add.accept(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + dy + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            add.accept(new Slot(inventory, col, INV_X + col * 18, HOTBAR_Y + dy));
        }
    }

    /** Setzt einen int aus zwei 16-Bit-Datenslots zusammen (ContainerData synchronisiert nur shorts). */
    public static int joined(ContainerData data, int lo, int hi) {
        return (data.get(lo) & 0xFFFF) | ((data.get(hi) & 0xFFFF) << 16);
    }

    /** Standard-Shift-Klick zwischen Spielerinventar und Hotbar. */
    public static boolean moveWithinPlayer(AbstractContainerMenu menu, int index, int invStart, Mover mover) {
        int hotbarStart = invStart + 27, end = hotbarStart + 9;
        if (index < hotbarStart) return mover.move(hotbarStart, end, false);
        return mover.move(invStart, hotbarStart, false);
    }

    @FunctionalInterface
    public interface Mover {
        boolean move(int start, int end, boolean reverse);
    }

    private MenuHelper() {}
}
