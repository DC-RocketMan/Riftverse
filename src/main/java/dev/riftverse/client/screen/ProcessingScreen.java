package dev.riftverse.client.screen;

import dev.riftverse.client.GuiKit;
import dev.riftverse.tech.MachineKind;
import dev.riftverse.tech.menu.ProcessingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Verarbeitende Maschinen: Erzmühle, Walzbank, Legierungsofen, Garagenpresse, Xenit-Schmelze, Platinen-Ätzer … */
public class ProcessingScreen extends AbstractContainerScreen<ProcessingMenu> {
    private static final int BAR_X = 13, BAR_Y = 16, BAR_W = 8, BAR_H = 42;

    @Nullable
    private Button modeButton;

    public ProcessingScreen(ProcessingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.kind().hasModes()) {
            modeButton = addRenderableWidget(Button.builder(menu.kind().modeName(menu.mode()), b -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ProcessingMenu.BUTTON_MODE);
                }
            }).bounds(leftPos + 28, topPos + 55, 56, 14).build());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (modeButton != null) modeButton.setMessage(menu.kind().modeName(menu.mode()));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        MachineKind kind = menu.kind();
        GuiKit.panel(g, x, y, imageWidth, imageHeight);

        int n = kind.inputs();
        for (int i = 0; i < n; i++) GuiKit.slot(g, x + ProcessingMenu.inputX(n, i), y + ProcessingMenu.inputY(n, i));
        int o = kind.outputs();
        for (int i = 0; i < o; i++) {
            int sx = x + ProcessingMenu.outputX(o, i), sy = y + ProcessingMenu.outputY(o, i);
            if (ProcessingMenu.bigOutput(o, i)) GuiKit.bigSlot(g, sx, sy);
            else GuiKit.slot(g, sx, sy);
        }
        GuiKit.arrow(g, x + ProcessingMenu.ARROW_X, y + ProcessingMenu.ARROW_Y, (float) menu.progress() / menu.maxProgress());

        if (kind.electric()) {
            GuiKit.energyBar(g, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, menu.energy(), menu.capacity());
            GuiKit.slot(g, x + ProcessingMenu.BATTERY_X, y + ProcessingMenu.BATTERY_Y);
        } else {
            // Garagenpresse: Kurbel-Symbol, leuchtet beim Kurbeln
            int c = menu.cranking() ? GuiKit.GREEN : GuiKit.SHADOW;
            g.fill(x + 14, y + 30, x + 20, y + 32, c);
            g.fill(x + 18, y + 32, x + 20, y + 42, c);
            g.fill(x + 10, y + 40, x + 18, y + 42, c);
        }
        for (int i = 0; i < 9 * 4; i++) {
            int col = i % 9, row = i / 9;
            GuiKit.slot(g, x + 8 + col * 18, y + (row < 3 ? 84 + row * 18 : 142));
        }
    }

    /** Präzisions-Montierer: der aktuelle Montageschritt unter dem Pfeil. */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        MachineKind kind = menu.kind();
        if (!kind.ordered()) return;
        int steps = menu.steps();
        if (steps > 0) {
            int step = Math.min(steps, 1 + menu.progress() * steps / menu.maxProgress());
            Component text = Component.translatable("gui.riftverse.schritt", step, steps);
            g.drawString(font, text, ProcessingMenu.ARROW_X + 11 - font.width(text) / 2, ProcessingMenu.ARROW_Y + 18, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = leftPos, y = topPos;
        if (menu.kind().electric() && GuiKit.inside(mouseX, mouseY, x + BAR_X, y + BAR_Y, BAR_W, BAR_H)) {
            g.renderComponentTooltip(font, EnergyTooltips.energy(menu.energy(), menu.capacity(), menu.kind().tier()), mouseX, mouseY);
        } else if (GuiKit.inside(mouseX, mouseY, x + ProcessingMenu.ARROW_X, y + ProcessingMenu.ARROW_Y, 22, 15)) {
            Component line = menu.kind().electric()
                    ? Component.translatable("tooltip.riftverse.consumption", menu.kind().energyPerTick())
                    : Component.translatable("tooltip.riftverse.crank_hint");
            List<Component> lines = new java.util.ArrayList<>(List.of(Component.translatable("tooltip.riftverse.progress",
                    menu.progress() * 100 / menu.maxProgress()), line));
            if (menu.kind().ordered()) lines.add(Component.translatable("tooltip.riftverse.montierer_reihenfolge"));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (!menu.kind().electric() && GuiKit.inside(mouseX, mouseY, x + 8, y + 28, 14, 16)) {
            g.renderComponentTooltip(font, List.of(Component.translatable("tooltip.riftverse.crank_hint")), mouseX, mouseY);
        }
    }
}
