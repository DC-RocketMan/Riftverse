package dev.riftverse.item;

import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModDataComponents.PeilerZiel;
import dev.riftverse.worldgen.KraterOrte;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

/**
 * Meteoritenpeiler: Die Nadel zeigt zum nächsten Meteoritenkrater und wird innerhalb der Xenit-Zone
 * (200 Blöcke) grün. Funktioniert nur in der Overworld; sonst dreht sich die Nadel wie ein Kompass im Nether.
 * <p>
 * Das Ziel wird jede Sekunde auf dem Server berechnet (ohne Chunks zu laden, siehe {@link KraterOrte}) und
 * am Item gespeichert; die Nadel liest es clientseitig.
 */
public class MeteoritenpeilerItem extends Item {
    /** Suchradius in Regionen (je 992 Blöcke). */
    private static final int SEARCH_REGIONS = 3;

    public MeteoritenpeilerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel sl) || !(entity instanceof Player) || entity.tickCount % 20 != 0) return;
        BlockPos pos = entity.blockPosition();
        BlockPos crater = KraterOrte.nearest(sl, pos.getX(), pos.getZ(), SEARCH_REGIONS);
        if (crater == null) {
            if (stack.has(ModDataComponents.PEILER.get())) stack.remove(ModDataComponents.PEILER.get());
            return;
        }
        boolean zone = KraterOrte.horizontal(crater, entity.getX(), entity.getZ()) <= KraterOrte.ZONE;
        PeilerZiel ziel = new PeilerZiel(GlobalPos.of(Level.OVERWORLD, crater), zone);
        if (!Objects.equals(stack.get(ModDataComponents.PEILER.get()), ziel)) stack.set(ModDataComponents.PEILER.get(), ziel);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PeilerZiel ziel = stack.get(ModDataComponents.PEILER.get());
        if (ziel == null) {
            tooltip.add(Component.translatable("tooltip.riftverse.peiler_kein_ziel").withStyle(ChatFormatting.GRAY));
        } else if (ziel.zone()) {
            tooltip.add(Component.translatable("tooltip.riftverse.peiler_zone").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.riftverse.peiler_ziel").withStyle(ChatFormatting.GRAY));
        }
    }
}
