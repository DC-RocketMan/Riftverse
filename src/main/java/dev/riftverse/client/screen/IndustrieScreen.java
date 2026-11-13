package dev.riftverse.client.screen;

import dev.riftverse.client.GuiKit;
import dev.riftverse.tech.IndustrieBlockEntity;
import dev.riftverse.tech.menu.IndustrieMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Gemeinsame GUI der T3-Geräte: Graphit-Leiste oben, Energiebalken links, Tank rechts. */
public class IndustrieScreen extends AbstractContainerScreen<IndustrieMenu> {
    private static final int BAR_X = 13, BAR_Y = 16, BAR_W = 8, BAR_H = 42;
    private static final int TANK_X = 156, TANK_Y = 16, TANK_W = 12, TANK_H = 58;
    private static final int HBAR_W = 96, HBAR_H = 6;

    private final List<Button> buttons = new ArrayList<>();
    private final List<IndustrieBlockEntity.GuiButton> specs = new ArrayList<>();

    public IndustrieScreen(IndustrieMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        specs.clear();
        IndustrieBlockEntity m = menu.machine();
        if (m == null) return;
        for (IndustrieBlockEntity.GuiButton spec : m.guiButtons()) {
            Button b = addRenderableWidget(Button.builder(m.buttonLabel(spec.id(), menu.extras()), btn -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, spec.id());
                }
            }).bounds(leftPos + spec.x(), topPos + spec.y(), spec.width(), 14).build());
            buttons.add(b);
            specs.add(spec);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        IndustrieBlockEntity m = menu.machine();
        if (m == null) return;
        int[] extras = menu.extras();
        for (int i = 0; i < buttons.size(); i++) buttons.get(i).setMessage(m.buttonLabel(specs.get(i).id(), extras));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GuiKit.panel(g, x, y, imageWidth, imageHeight);
        IndustrieBlockEntity m = menu.machine();

        GuiKit.energyBar(g, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, menu.energy(), menu.capacity());
        if (m != null) {
            for (IndustrieBlockEntity.GuiSlot s : m.guiSlots()) {
                if (s.big()) GuiKit.bigSlot(g, x + s.x(), y + s.y());
                else GuiKit.slot(g, x + s.x(), y + s.y());
            }
            if (m.batterySlot() >= 0) GuiKit.slot(g, x + IndustrieBlockEntity.BATTERY_X, y + IndustrieBlockEntity.BATTERY_Y);
            float frac = (float) menu.work() / menu.workMax();
            switch (m.progress()) {
                case ARROW -> GuiKit.arrow(g, x + m.progressX(), y + m.progressY(), frac);
                case BAR -> GuiKit.hbar(g, x + m.progressX(), y + m.progressY(), HBAR_W, HBAR_H, frac, GuiKit.GREEN);
                case NONE -> { }
            }
            if (m.hasTank()) {
                GuiKit.inset(g, x + TANK_X - 1, y + TANK_Y - 1, TANK_W + 2, TANK_H + 2);
                g.fill(x + TANK_X, y + TANK_Y, x + TANK_X + TANK_W, y + TANK_Y + TANK_H, 0xFF101418);
                int filled = (int) ((long) TANK_H * menu.fluid() / menu.fluidCapacity());
                if (filled > 0) {
                    g.fill(x + TANK_X, y + TANK_Y + TANK_H - filled, x + TANK_X + TANK_W, y + TANK_Y + TANK_H, m.tankColor());
                    g.fill(x + TANK_X + 2, y + TANK_Y + TANK_H - filled, x + TANK_X + 4, y + TANK_Y + TANK_H, 0x55FFFFFF);
                }
                for (int yy = y + TANK_Y + 6; yy < y + TANK_Y + TANK_H; yy += 8) g.fill(x + TANK_X, yy, x + TANK_X + 3, yy + 1, 0x88000000);
            }
        }
        for (int i = 0; i < 9 * 4; i++) {
            int col = i % 9, row = i / 9;
            GuiKit.slot(g, x + 8 + col * 18, y + (row < 3 ? 84 + row * 18 : 142));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        IndustrieBlockEntity m = menu.machine();
        if (m == null) return;
        int yy = m.infoY();
        for (Component line : m.infoLines(menu.extras())) {
            g.drawString(font, line, m.infoX(), yy, 0x404040, false);
            yy += 10;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = leftPos, y = topPos;
        IndustrieBlockEntity m = menu.machine();
        if (m == null) return;
        if (GuiKit.inside(mouseX, mouseY, x + BAR_X, y + BAR_Y, BAR_W, BAR_H)) {
            g.renderComponentTooltip(font, EnergyTooltips.energy(menu.energy(), menu.capacity(), m.guiTier()), mouseX, mouseY);
            return;
        }
        if (m.hasTank() && GuiKit.inside(mouseX, mouseY, x + TANK_X, y + TANK_Y, TANK_W, TANK_H)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("tooltip.riftverse.tank",
                    Component.translatable("fluid_type.riftverse.portalfluid"), String.format(Locale.GERMANY, "%,d", menu.fluid()), String.format(Locale.GERMANY, "%,d", menu.fluidCapacity()))),
                    mouseX, mouseY);
            return;
        }
        boolean overProgress = switch (m.progress()) {
            case ARROW -> GuiKit.inside(mouseX, mouseY, x + m.progressX(), y + m.progressY(), 22, 15);
            case BAR -> GuiKit.inside(mouseX, mouseY, x + m.progressX(), y + m.progressY(), HBAR_W, HBAR_H);
            case NONE -> false;
        };
        if (overProgress) g.renderComponentTooltip(font, m.progressTooltip(menu.work(), menu.workMax(), menu.extras()), mouseX, mouseY);
    }
}
