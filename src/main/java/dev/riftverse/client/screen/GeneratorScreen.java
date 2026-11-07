package dev.riftverse.client.screen;

import dev.riftverse.client.GuiKit;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.tech.KurbelDynamoBlockEntity;
import dev.riftverse.tech.TonnenreaktorBlockEntity;
import dev.riftverse.tech.menu.GeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Kurbel-Dynamo und Tonnenreaktor. */
public class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {
    private static final int BAR_X = 13, BAR_Y = 16, BAR_W = 8, BAR_H = 42;
    private static final int SWING_X = 40, SWING_Y = 34, SWING_W = 96, SWING_H = 6;
    private static final int FLAME_X = 57, FLAME_Y = 36;

    public GeneratorScreen(GeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    private int output() {
        return menu.isReactor() ? TonnenreaktorBlockEntity.OUTPUT : KurbelDynamoBlockEntity.OUTPUT;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GuiKit.panel(g, x, y, imageWidth, imageHeight);
        float work = (float) menu.work() / menu.workMax();

        if (menu.isReactor()) {
            GuiKit.energyBar(g, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, menu.energy(), menu.capacity());
            GuiKit.slot(g, x + GeneratorMenu.BATTERY_X, y + GeneratorMenu.BATTERY_Y);
            GuiKit.slot(g, x + GeneratorMenu.FUEL_X, y + GeneratorMenu.FUEL_Y);
            GuiKit.flame(g, x + FLAME_X, y + FLAME_Y, menu.work() > 0 ? work : 0);
            GuiKit.arrow(g, x + 82, y + 35, menu.work() > 0 ? 1.0f : 0.0f);
            GuiKit.slot(g, x + GeneratorMenu.OUT_X, y + GeneratorMenu.OUT_Y);
        } else {
            GuiKit.energyBar(g, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, menu.energy(), menu.capacity());
            GuiKit.hbar(g, x + SWING_X, y + SWING_Y, SWING_W, SWING_H, work, GuiKit.GREEN);
            GuiKit.slot(g, x + GeneratorMenu.DYNAMO_BATTERY_X, y + GeneratorMenu.DYNAMO_BATTERY_Y);
        }
        for (int i = 0; i < 9 * 4; i++) {
            int col = i % 9, row = i / 9;
            GuiKit.slot(g, x + 8 + col * 18, y + (row < 3 ? 84 + row * 18 : 142));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        if (!menu.isReactor()) {
            g.drawString(font, Component.translatable("gui.riftverse.momentum"), SWING_X, SWING_Y - 11, 0x404040, false);
        } else {
            g.drawString(font, Component.translatable("gui.riftverse.parts"), GeneratorMenu.OUT_X - 6, GeneratorMenu.OUT_Y + 20, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = leftPos, y = topPos;
        if (GuiKit.inside(mouseX, mouseY, x + BAR_X, y + BAR_Y, BAR_W, BAR_H)) {
            g.renderComponentTooltip(font, EnergyTooltips.energy(menu.energy(), menu.capacity(), VoltageTier.forPacket(output())), mouseX, mouseY);
        } else if (!menu.isReactor() && GuiKit.inside(mouseX, mouseY, x + SWING_X, y + SWING_Y, SWING_W, SWING_H)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("tooltip.riftverse.crank_hint"),
                    Component.translatable("tooltip.riftverse.output", output())), mouseX, mouseY);
        } else if (menu.isReactor() && GuiKit.inside(mouseX, mouseY, x + FLAME_X, y + FLAME_Y, 14, 14)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("tooltip.riftverse.reactor_fuel"),
                    Component.translatable("tooltip.riftverse.output", output())), mouseX, mouseY);
        }
    }
}
