package dev.riftverse.client.screen;

import dev.riftverse.client.GuiKit;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.tech.RaffinerieBlockEntity;
import dev.riftverse.tech.menu.RaffinerieMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Portalfluid-Raffinerie: Roh-Portalfluid links, stabilisiertes rechts, dazwischen Säuregel und Fluxstaub. */
public class RaffinerieScreen extends AbstractContainerScreen<RaffinerieMenu> {
    private static final int BAR_X = 13, BAR_Y = 16, BAR_W = 8, BAR_H = 42;
    private static final int RAW = GuiKit.GREEN, STABIL = 0xFF9A6BFF;

    public RaffinerieScreen(RaffinerieMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GuiKit.panel(g, x, y, imageWidth, imageHeight);
        GuiKit.energyBar(g, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, menu.energy(), menu.capacity());
        GuiKit.slot(g, x + RaffinerieMenu.BATTERY_X, y + RaffinerieMenu.BATTERY_Y);
        tank(g, x + RaffinerieMenu.RAW_TANK_X, y + RaffinerieMenu.TANK_Y, menu.raw(), RAW);
        tank(g, x + RaffinerieMenu.STABIL_TANK_X, y + RaffinerieMenu.TANK_Y, menu.stabil(), STABIL);
        GuiKit.slot(g, x + RaffinerieMenu.RAW_IN_X, y + RaffinerieMenu.RAW_IN_Y);
        GuiKit.slot(g, x + RaffinerieMenu.RAW_OUT_X, y + RaffinerieMenu.RAW_OUT_Y);
        GuiKit.slot(g, x + RaffinerieMenu.IN_A_X, y + RaffinerieMenu.IN_Y);
        GuiKit.slot(g, x + RaffinerieMenu.IN_B_X, y + RaffinerieMenu.IN_Y);
        GuiKit.slot(g, x + RaffinerieMenu.CART_IN_X, y + RaffinerieMenu.CART_IN_Y);
        GuiKit.slot(g, x + RaffinerieMenu.CART_OUT_X, y + RaffinerieMenu.CART_OUT_Y);
        GuiKit.arrow(g, x + RaffinerieMenu.ARROW_X, y + RaffinerieMenu.ARROW_Y, (float) menu.progress() / menu.maxProgress());
        g.fill(x + RaffinerieMenu.CART_IN_X + 7, y + 35, x + RaffinerieMenu.CART_IN_X + 9, y + 41, 0xFF8B8B8B);
        g.fill(x + RaffinerieMenu.RAW_IN_X + 7, y + 35, x + RaffinerieMenu.RAW_IN_X + 9, y + 41, 0xFF8B8B8B);
        for (int i = 0; i < 9 * 4; i++) {
            int col = i % 9, row = i / 9;
            GuiKit.slot(g, x + 8 + col * 18, y + (row < 3 ? 84 + row * 18 : 142));
        }
    }

    private static void tank(GuiGraphics g, int x, int y, int amount, int color) {
        GuiKit.inset(g, x - 1, y - 1, RaffinerieMenu.TANK_W + 2, RaffinerieMenu.TANK_H + 2);
        g.fill(x, y, x + RaffinerieMenu.TANK_W, y + RaffinerieMenu.TANK_H, 0xFF101418);
        int h = (int) ((long) RaffinerieMenu.TANK_H * amount / RaffinerieBlockEntity.TANK);
        if (h > 0) g.fill(x, y + RaffinerieMenu.TANK_H - h, x + RaffinerieMenu.TANK_W, y + RaffinerieMenu.TANK_H, color);
        for (int yy = y + 6; yy < y + RaffinerieMenu.TANK_H; yy += 7) g.fill(x, yy, x + 3, yy + 1, 0x66000000);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        if (!menu.formed()) {
            Component warn = Component.translatable("gui.riftverse.struktur_unvollstaendig");
            g.drawString(font, warn, imageWidth - 8 - font.width(warn), inventoryLabelY, 0xB03000, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int x = leftPos, y = topPos;
        if (GuiKit.inside(mouseX, mouseY, x + BAR_X, y + BAR_Y, BAR_W, BAR_H)) {
            g.renderComponentTooltip(font, EnergyTooltips.energy(menu.energy(), menu.capacity(), VoltageTier.II), mouseX, mouseY);
        } else if (GuiKit.inside(mouseX, mouseY, x + RaffinerieMenu.RAW_TANK_X, y + RaffinerieMenu.TANK_Y, RaffinerieMenu.TANK_W, RaffinerieMenu.TANK_H)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("tooltip.riftverse.tank", Component.translatable("fluid_type.riftverse.roh_portalfluid"),
                    EnergyTooltips.fmt(menu.raw()), EnergyTooltips.fmt(RaffinerieBlockEntity.TANK)));
            int rest = menu.restSeconds();
            if (rest >= 0) lines.add(Component.translatable("tooltip.riftverse.zerfaellt_in", String.format(Locale.ROOT, "%d:%02d", rest / 60, rest % 60)));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (GuiKit.inside(mouseX, mouseY, x + RaffinerieMenu.STABIL_TANK_X, y + RaffinerieMenu.TANK_Y, RaffinerieMenu.TANK_W, RaffinerieMenu.TANK_H)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("tooltip.riftverse.tank",
                    Component.translatable("fluid_type.riftverse.portalfluid"), EnergyTooltips.fmt(menu.stabil()),
                    EnergyTooltips.fmt(RaffinerieBlockEntity.TANK))), mouseX, mouseY);
        } else if (GuiKit.inside(mouseX, mouseY, x + RaffinerieMenu.ARROW_X, y + RaffinerieMenu.ARROW_Y, 22, 15)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("tooltip.riftverse.progress", menu.progress() * 100 / menu.maxProgress()));
            lines.add(Component.translatable("tooltip.riftverse.consumption", RaffinerieBlockEntity.CONSUMPTION));
            if (!menu.formed()) lines.add(Component.translatable("tooltip.riftverse.multiblock_rechtsklick"));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
}
