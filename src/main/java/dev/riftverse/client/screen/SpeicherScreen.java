package dev.riftverse.client.screen;

import dev.riftverse.client.GuiKit;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.tech.menu.SpeicherMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Xenit-Zelle: großer Füllstandsbalken, darunter Slots zum Laden und Entladen von Items. */
public class SpeicherScreen extends AbstractContainerScreen<SpeicherMenu> {
    private static final int BAR_X = 30, BAR_Y = 22, BAR_W = 116, BAR_H = 10;

    public SpeicherScreen(SpeicherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GuiKit.panel(g, x, y, imageWidth, imageHeight);
        GuiKit.hbar(g, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, (float) menu.energy() / menu.capacity(), GuiKit.GREEN);
        GuiKit.slot(g, x + SpeicherMenu.CHARGE_X, y + SpeicherMenu.SLOT_Y);
        GuiKit.slot(g, x + SpeicherMenu.DISCHARGE_X, y + SpeicherMenu.SLOT_Y);
        for (int i = 0; i < 9 * 4; i++) {
            int col = i % 9, row = i / 9;
            GuiKit.slot(g, x + 8 + col * 18, y + (row < 3 ? 84 + row * 18 : 142));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        Component charge = Component.translatable("gui.riftverse.laden");
        Component discharge = Component.translatable("gui.riftverse.entladen");
        g.drawString(font, charge, SpeicherMenu.CHARGE_X + 8 - font.width(charge) / 2, SpeicherMenu.SLOT_Y - 11, 0x404040, false);
        g.drawString(font, discharge, SpeicherMenu.DISCHARGE_X + 8 - font.width(discharge) / 2, SpeicherMenu.SLOT_Y - 11, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (GuiKit.inside(mouseX, mouseY, leftPos + BAR_X, topPos + BAR_Y, BAR_W, BAR_H)) {
            g.renderComponentTooltip(font, EnergyTooltips.energy(menu.energy(), menu.capacity(), menu.tier()), mouseX, mouseY);
        }
    }
}
