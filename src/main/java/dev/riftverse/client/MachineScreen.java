package dev.riftverse.client;

import dev.riftverse.Riftverse;
import dev.riftverse.machine.MachineMenu;
import dev.riftverse.machine.MachineRecipes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final ResourceLocation TEXTURE = Riftverse.id("textures/gui/machine.png");

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Farbstreifen je Maschinentyp
        int color = 0xFF000000 | menu.machineType().color();
        g.fill(x + 160, y + 17, x + 164, y + 69, color);

        if (menu.isBurning()) {
            int h = menu.burnScaled(13) + 1;
            g.blit(TEXTURE, x + 51, y + 36 + 14 - h, 176, 14 - h, 14, h);
        }
        int w = menu.progressScaled(24);
        g.blit(TEXTURE, x + 86, y + 34, 176, 14, w + 1, 17);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        // Rezept-Hilfe über dem Fortschrittspfeil
        if (mouseX >= leftPos + 86 && mouseX < leftPos + 110 && mouseY >= topPos + 34 && mouseY < topPos + 51) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.riftverse.recipes"));
            for (MachineRecipes.Recipe r : MachineRecipes.forMachine(menu.machineType())) {
                String b = r.inputB() != null ? " + " + r.countB() + "× " + r.inputB().get().asItem().getDescription().getString() : "";
                lines.add(Component.literal(r.countA() + "× " + r.inputA().get().asItem().getDescription().getString() + b
                        + " → " + r.outputCount() + "× " + r.output().get().asItem().getDescription().getString()));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
}
