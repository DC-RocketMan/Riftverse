package dev.riftverse.client.screen;

import dev.riftverse.client.GuiKit;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.tech.FluidMixerBlockEntity;
import dev.riftverse.tech.menu.FluidMixerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Fluid-Mixer: Wassertank, Fortschritt, Tank mit Roh-Portalfluid und Kartuschen. */
public class FluidMixerScreen extends AbstractContainerScreen<FluidMixerMenu> {
    private static final int BAR_X = 13, BAR_Y = 16, BAR_W = 8, BAR_H = 42;
    private static final int WATER = 0xFF3F76E4, PORTAL = GuiKit.GREEN;

    public FluidMixerScreen(FluidMixerMenu menu, Inventory inventory, Component title) {
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
        GuiKit.slot(g, x + FluidMixerMenu.BATTERY_X, y + FluidMixerMenu.BATTERY_Y);

        tank(g, x + FluidMixerMenu.WATER_X, y + FluidMixerMenu.TANK_Y, menu.water(), WATER);
        tank(g, x + FluidMixerMenu.OUTPUT_X, y + FluidMixerMenu.TANK_Y, menu.output(), PORTAL);

        GuiKit.slot(g, x + FluidMixerMenu.INPUT_X, y + FluidMixerMenu.INPUT_Y);
        GuiKit.slot(g, x + FluidMixerMenu.BUCKET_IN_X, y + FluidMixerMenu.BUCKET_IN_Y);
        GuiKit.slot(g, x + FluidMixerMenu.BUCKET_OUT_X, y + FluidMixerMenu.BUCKET_OUT_Y);
        GuiKit.slot(g, x + FluidMixerMenu.CART_IN_X, y + FluidMixerMenu.CART_IN_Y);
        GuiKit.slot(g, x + FluidMixerMenu.CART_OUT_X, y + FluidMixerMenu.CART_OUT_Y);
        GuiKit.arrow(g, x + FluidMixerMenu.ARROW_X, y + FluidMixerMenu.ARROW_Y, (float) menu.progress() / menu.maxProgress());
        // kleiner Pfeil von der leeren zur vollen Kartusche
        g.fill(x + FluidMixerMenu.CART_IN_X + 7, y + 35, x + FluidMixerMenu.CART_IN_X + 9, y + 41, 0xFF8B8B8B);

        for (int i = 0; i < 9 * 4; i++) {
            int col = i % 9, row = i / 9;
            GuiKit.slot(g, x + 8 + col * 18, y + (row < 3 ? 84 + row * 18 : 142));
        }
    }

    private static void tank(GuiGraphics g, int x, int y, int amount, int color) {
        GuiKit.inset(g, x - 1, y - 1, FluidMixerMenu.TANK_W + 2, FluidMixerMenu.TANK_H + 2);
        g.fill(x, y, x + FluidMixerMenu.TANK_W, y + FluidMixerMenu.TANK_H, 0xFF101418);
        int h = (int) ((long) FluidMixerMenu.TANK_H * amount / FluidMixerBlockEntity.TANK);
        if (h > 0) g.fill(x, y + FluidMixerMenu.TANK_H - h, x + FluidMixerMenu.TANK_W, y + FluidMixerMenu.TANK_H, color);
        for (int yy = y + 6; yy < y + FluidMixerMenu.TANK_H; yy += 7) g.fill(x, yy, x + 3, yy + 1, 0x66000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int x = leftPos, y = topPos;
        if (GuiKit.inside(mouseX, mouseY, x + BAR_X, y + BAR_Y, BAR_W, BAR_H)) {
            g.renderComponentTooltip(font, EnergyTooltips.energy(menu.energy(), menu.capacity(), VoltageTier.I), mouseX, mouseY);
        } else if (GuiKit.inside(mouseX, mouseY, x + FluidMixerMenu.WATER_X, y + FluidMixerMenu.TANK_Y, FluidMixerMenu.TANK_W, FluidMixerMenu.TANK_H)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("tooltip.riftverse.tank",
                    Component.translatable("fluid_type.minecraft.water"), fmt(menu.water()), fmt(FluidMixerBlockEntity.TANK))), mouseX, mouseY);
        } else if (GuiKit.inside(mouseX, mouseY, x + FluidMixerMenu.OUTPUT_X, y + FluidMixerMenu.TANK_Y, FluidMixerMenu.TANK_W, FluidMixerMenu.TANK_H)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("tooltip.riftverse.tank", Component.translatable("fluid_type.riftverse.roh_portalfluid"),
                    fmt(menu.output()), fmt(FluidMixerBlockEntity.TANK)));
            int rest = menu.restSeconds();
            if (rest >= 0) {
                lines.add(Component.translatable("tooltip.riftverse.zerfaellt_in",
                        String.format(Locale.ROOT, "%d:%02d", rest / 60, rest % 60)));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (GuiKit.inside(mouseX, mouseY, x + FluidMixerMenu.ARROW_X, y + FluidMixerMenu.ARROW_Y, 22, 15)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("tooltip.riftverse.progress",
                    menu.progress() * 100 / menu.maxProgress()),
                    Component.translatable("tooltip.riftverse.consumption", FluidMixerBlockEntity.CONSUMPTION)), mouseX, mouseY);
        }
    }

    private static String fmt(int value) {
        return EnergyTooltips.fmt(value);
    }
}
