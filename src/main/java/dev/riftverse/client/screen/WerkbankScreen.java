package dev.riftverse.client.screen;

import dev.riftverse.client.GuiKit;
import dev.riftverse.registry.ModItems;
import dev.riftverse.tech.menu.WerkbankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Garagen-Werkbank: Raster links, Ergebnis rechts, Werkzeugspalte ganz rechts. */
public class WerkbankScreen extends AbstractContainerScreen<WerkbankMenu> {
    private static final String[] TOOL_KEYS = {
            "item.riftverse.garagen_hammer", "item.riftverse.loetkolben", "item.riftverse.loetzinn"};

    public WerkbankScreen(WerkbankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    private ItemStack ghost(int i) {
        return switch (i) {
            case 0 -> new ItemStack(ModItems.GARAGEN_HAMMER.get());
            case 1 -> new ItemStack(ModItems.LOETKOLBEN.get());
            default -> new ItemStack(ModItems.LOETZINN.get());
        };
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GuiKit.panel(g, x, y, imageWidth, imageHeight);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                GuiKit.slot(g, x + WerkbankMenu.GRID_X + col * 18, y + WerkbankMenu.GRID_Y + row * 18);
            }
        }
        GuiKit.arrow(g, x + 90, y + 35, 0);
        GuiKit.bigSlot(g, x + WerkbankMenu.RESULT_X, y + WerkbankMenu.RESULT_Y);

        // Werkzeugleiste in Rost-Optik (T0)
        g.fill(x + WerkbankMenu.TOOL_X - 3, y + WerkbankMenu.TOOL_Y - 3, x + WerkbankMenu.TOOL_X + 19, y + WerkbankMenu.TOOL_Y + 55, 0xFF8A4B2A);
        for (int i = 0; i < 3; i++) {
            int sx = x + WerkbankMenu.TOOL_X, sy = y + WerkbankMenu.TOOL_Y + i * 18;
            GuiKit.slot(g, sx, sy);
            if (!menu.getSlot(WerkbankMenu.TOOL_START + i).hasItem()) {
                g.renderFakeItem(ghost(i), sx, sy);
                g.pose().pushPose();
                g.pose().translate(0, 0, 300);
                g.fill(sx, sy, sx + 16, sy + 16, 0xAA8B8B8B);
                g.pose().popPose();
            }
        }
        for (int i = 0; i < 9 * 4; i++) {
            int col = i % 9, row = i / 9;
            GuiKit.slot(g, x + 8 + col * 18, y + (row < 3 ? 84 + row * 18 : 142));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        for (int i = 0; i < 3; i++) {
            int sx = leftPos + WerkbankMenu.TOOL_X, sy = topPos + WerkbankMenu.TOOL_Y + i * 18;
            if (!menu.getSlot(WerkbankMenu.TOOL_START + i).hasItem() && GuiKit.inside(mouseX, mouseY, sx, sy, 16, 16)) {
                g.renderComponentTooltip(font, List.of(Component.translatable(TOOL_KEYS[i]),
                        Component.translatable("tooltip.riftverse.werkbank_tool")), mouseX, mouseY);
            }
        }
    }
}
