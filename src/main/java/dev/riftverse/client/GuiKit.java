package dev.riftverse.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Zeichnet die Riftverse-GUIs ohne Texturdatei: Vanilla-Paneel, Slots, Energiebalken, Pfeil.
 * Farben folgen dem Design-Leitfaden (Portal-Grün nur für "aktiv").
 */
public final class GuiKit {
    public static final int PANEL = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADOW = 0xFF555555, BORDER = 0xFF000000;
    public static final int SLOT = 0xFF8B8B8B, SLOT_DARK = 0xFF373737;
    public static final int GREEN = 0xFF39FF6A, GREEN_DARK = 0xFF12A84A, PETROL = 0xFF1E5A61, ORANGE = 0xFFFF8A1E;

    /** Vanilla-artiges Paneel mit abgerundet wirkendem Rand. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, BORDER);
        g.fill(x, y + 1, x + w, y + h - 1, BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, SHADOW);
    }

    /** Slot-Rahmen für einen Slot, dessen Item bei (x, y) liegt. */
    public static void slot(GuiGraphics g, int x, int y) {
        inset(g, x - 1, y - 1, 18, 18);
    }

    /** Großer Ergebnis-Slot (26×26). */
    public static void bigSlot(GuiGraphics g, int x, int y) {
        inset(g, x - 5, y - 5, 26, 26);
    }

    public static void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, SLOT);
        g.fill(x, y, x + w - 1, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + h - 1, SLOT_DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, LIGHT);
    }

    /** Senkrechter Energiebalken, füllt sich von unten. */
    public static void energyBar(GuiGraphics g, int x, int y, int w, int h, int stored, int capacity) {
        inset(g, x - 1, y - 1, w + 2, h + 2);
        g.fill(x, y, x + w, y + h, 0xFF101418);
        int filled = capacity <= 0 ? 0 : (int) ((long) h * stored / capacity);
        if (filled > 0) {
            g.fill(x, y + h - filled, x + w, y + h, GREEN_DARK);
            g.fill(x + 1, y + h - filled, x + w - 1, y + h, GREEN);
        }
        for (int yy = y + 4; yy < y + h; yy += 5) g.fill(x, yy, x + 2, yy + 1, 0x66000000);
    }

    /** Fortschrittspfeil nach rechts (22×15), {@code frac} von 0 bis 1. */
    public static void arrow(GuiGraphics g, int x, int y, float frac) {
        int body = 0xFF8B8B8B;
        g.fill(x, y + 5, x + 15, y + 10, body);
        for (int i = 0; i < 8; i++) g.fill(x + 15 + i, y + i, x + 16 + i, y + 15 - i, body);
        int fill = Math.round(22 * Math.max(0, Math.min(1, frac)));
        if (fill <= 0) return;
        g.fill(x, y + 5, x + Math.min(fill, 15), y + 10, LIGHT);
        for (int i = 0; i < 8 && 15 + i < fill; i++) g.fill(x + 15 + i, y + i, x + 16 + i, y + 15 - i, LIGHT);
    }

    /** Flamme (14×14), {@code frac} = verbleibende Brenndauer. */
    public static void flame(GuiGraphics g, int x, int y, float frac) {
        g.fill(x + 3, y + 2, x + 11, y + 14, 0xFF8B8B8B);
        int h = Math.round(12 * Math.max(0, Math.min(1, frac)));
        if (h <= 0) return;
        g.fill(x + 3, y + 14 - h, x + 11, y + 14, ORANGE);
        g.fill(x + 5, y + 14 - Math.max(1, h * 2 / 3), x + 9, y + 14, 0xFFFFD27A);
    }

    /** Waagrechter Balken, z. B. Schwung des Kurbel-Dynamos. */
    public static void hbar(GuiGraphics g, int x, int y, int w, int h, float frac, int color) {
        inset(g, x - 1, y - 1, w + 2, h + 2);
        g.fill(x, y, x + w, y + h, 0xFF101418);
        int fill = Math.round(w * Math.max(0, Math.min(1, frac)));
        if (fill > 0) g.fill(x, y, x + fill, y + h, color);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private GuiKit() {}
}
