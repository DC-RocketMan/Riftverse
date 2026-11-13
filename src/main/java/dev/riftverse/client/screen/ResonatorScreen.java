package dev.riftverse.client.screen;

import dev.riftverse.client.GuiKit;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.tech.FluxkristallResonatorBlockEntity;
import dev.riftverse.tech.menu.ResonatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Fluxkristall-Resonator: Raster, Hitzebalken (orange), Energie, Wassertank. */
public class ResonatorScreen extends AbstractContainerScreen<ResonatorMenu> {
    private static final int HEAT_X = 8, HEAT_Y = 82, ENERGY_Y = 92, BAR_W = 70, BAR_H = 5;
    private static final int WATER_X = 84, WATER_Y = 78, WATER_W = 24, WATER_H = 20;

    public ResonatorScreen(ResonatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166 + ResonatorMenu.INV_SHIFT;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GuiKit.panel(g, x, y, imageWidth, imageHeight);
        int cols = menu.columns();
        for (int row = 0; row < FluxkristallResonatorBlockEntity.ROWS; row++) {
            for (int col = 0; col < FluxkristallResonatorBlockEntity.MAX_COLUMNS; col++) {
                int sx = x + ResonatorMenu.GRID_X + col * 18, sy = y + ResonatorMenu.GRID_Y + row * 18;
                if (col < cols) GuiKit.slot(g, sx, sy);
                else g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF9A9A9A);   // noch keine Kammer
            }
        }
        float heat = (float) menu.heat() / FluxkristallResonatorBlockEntity.MAX_HEAT;
        GuiKit.hbar(g, x + HEAT_X, y + HEAT_Y, BAR_W, BAR_H, heat, heat > 0.75f ? 0xFFCF3030 : GuiKit.ORANGE);
        GuiKit.hbar(g, x + HEAT_X, y + ENERGY_Y, BAR_W, BAR_H, (float) menu.energy() / menu.capacity(), GuiKit.GREEN);
        GuiKit.inset(g, x + WATER_X - 1, y + WATER_Y - 1, WATER_W + 2, WATER_H + 2);
        g.fill(x + WATER_X, y + WATER_Y, x + WATER_X + WATER_W, y + WATER_Y + WATER_H, 0xFF101418);
        int h = WATER_H * menu.water() / FluxkristallResonatorBlockEntity.TANK;
        if (h > 0) g.fill(x + WATER_X, y + WATER_Y + WATER_H - h, x + WATER_X + WATER_W, y + WATER_Y + WATER_H, 0xFF3F76E4);
        GuiKit.slot(g, x + ResonatorMenu.BUCKET_IN_X, y + ResonatorMenu.LOWER_Y);
        GuiKit.slot(g, x + ResonatorMenu.BUCKET_OUT_X, y + ResonatorMenu.LOWER_Y);
        GuiKit.slot(g, x + ResonatorMenu.BATTERY_X, y + ResonatorMenu.LOWER_Y);
        int invY = y + 84 + ResonatorMenu.INV_SHIFT;
        for (int i = 0; i < 9 * 4; i++) {
            int col = i % 9, row = i / 9;
            GuiKit.slot(g, x + 8 + col * 18, row < 3 ? invY + row * 18 : invY + 58);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        Component out = Component.translatable("tooltip.riftverse.output", menu.output());
        g.drawString(font, out, HEAT_X, ENERGY_Y + 8, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int x = leftPos, y = topPos;
        if (GuiKit.inside(mouseX, mouseY, x + HEAT_X, y + HEAT_Y, BAR_W, BAR_H)) {
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("tooltip.riftverse.hitze", EnergyTooltips.fmt(menu.heat()),
                            EnergyTooltips.fmt(FluxkristallResonatorBlockEntity.MAX_HEAT)),
                    Component.translatable("tooltip.riftverse.hitze_delta", (menu.heatDelta() > 0 ? "+" : "") + menu.heatDelta()),
                    Component.translatable("tooltip.riftverse.resonator_regel")), mouseX, mouseY);
        } else if (GuiKit.inside(mouseX, mouseY, x + HEAT_X, y + ENERGY_Y, BAR_W, BAR_H)) {
            g.renderComponentTooltip(font, EnergyTooltips.energy(menu.energy(), menu.capacity(), VoltageTier.II), mouseX, mouseY);
        } else if (GuiKit.inside(mouseX, mouseY, x + WATER_X, y + WATER_Y, WATER_W, WATER_H)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("tooltip.riftverse.tank",
                    Component.translatable("fluid_type.minecraft.water"), EnergyTooltips.fmt(menu.water()),
                    EnergyTooltips.fmt(FluxkristallResonatorBlockEntity.TANK)),
                    Component.translatable("tooltip.riftverse.resonator_wasser")), mouseX, mouseY);
        }
    }
}
