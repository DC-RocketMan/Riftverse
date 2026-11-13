package dev.riftverse.compat;

import dev.riftverse.energy.RiftEnergyBlockEntity;
import dev.riftverse.energy.RiftEnergySink;
import dev.riftverse.energy.RiftEnergySource;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.tech.FluxkristallResonatorBlockEntity;
import dev.riftverse.tech.IndustrieBlockEntity;
import dev.riftverse.tech.ProcessingBlockEntity;
import dev.riftverse.tech.multiblock.MultiblockController;
import dev.riftverse.tech.upgrade.Aufruestbar;
import dev.riftverse.tech.upgrade.UpgradeChip;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Was Jade beim Anschauen eines Riftverse-Blocks zeigt: Spannungsstufe, FE (wo der Block keine FE-Schnittstelle hat),
 * Hitze, Zivilisationsstufe, Fortschritt, eingebaute Chips und den Zustand des Multiblocks. Fluide, Items und FE mit
 * Schnittstelle zeigt Jade selbst. Läuft auf dem Server.
 */
public final class BlockInfo {
    private BlockInfo() {
    }

    public static List<Component> zeilen(BlockEntity be) {
        List<Component> out = new ArrayList<>();
        Level level = be.getLevel();

        VoltageTier ein = be instanceof RiftEnergySink sink ? sink.getSinkTier() : null;
        VoltageTier aus = be instanceof RiftEnergySource src ? src.getSourceTier() : null;
        if (ein != null && ein == aus) {
            out.add(Component.translatable("jade.riftverse.spannung", ein.describe()));
        } else {
            if (ein != null) out.add(Component.translatable("jade.riftverse.spannung_ein", ein.describe()));
            if (aus != null) out.add(Component.translatable("jade.riftverse.spannung_aus", aus.displayName()));
        }

        if (be instanceof RiftEnergyBlockEntity r && r.energy().capacity() > 1 && level != null
                && level.getCapability(Capabilities.EnergyStorage.BLOCK, be.getBlockPos(), null) == null) {
            out.add(Component.translatable("jade.riftverse.energie", zahl(r.energy().stored()), zahl(r.energy().capacity())));
        }

        if (be instanceof FluxkristallResonatorBlockEntity res) {
            int heat = res.heat();
            ChatFormatting farbe = heat > FluxkristallResonatorBlockEntity.MAX_HEAT * 3 / 4 ? ChatFormatting.RED
                    : heat > FluxkristallResonatorBlockEntity.MAX_HEAT / 2 ? ChatFormatting.GOLD : ChatFormatting.WHITE;
            out.add(Component.translatable("jade.riftverse.hitze", zahl(heat), zahl(FluxkristallResonatorBlockEntity.MAX_HEAT))
                    .withStyle(farbe));
        }

        if (be instanceof ProcessingBlockEntity p) {
            if (p.arbeit() > 0) {
                out.add(Component.translatable("tooltip.riftverse.progress", p.arbeit() * 100 / Math.max(1, p.arbeitMax())));
            }
            if (p.krise() > 0) out.add(Component.translatable("tooltip.riftverse.ki_krise").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (be instanceof IndustrieBlockEntity i) out.addAll(i.statusZeilen());

        if (be instanceof Aufruestbar a && !a.chips().isEmpty()) {
            MutableComponent chips = Component.empty();
            boolean erst = true;
            for (UpgradeChip c : a.chips()) {
                if (!erst) chips.append(", ");
                chips.append(Component.translatable("item.riftverse." + c.itemName()));
                erst = false;
            }
            out.add(Component.translatable("jade.riftverse.chips", chips).withStyle(ChatFormatting.AQUA));
        }

        boolean einzeln = be instanceof ProcessingBlockEntity pb && pb.kind().multiblock() == null;
        if (be instanceof MultiblockController mb && !einzeln) {
            Component fehlt = mb.checkStructure();
            out.add(fehlt == null
                    ? Component.translatable("jade.riftverse.struktur_ok").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("jade.riftverse.struktur_fehlt", fehlt).withStyle(ChatFormatting.GOLD));
        }
        return out;
    }

    public static String zahl(long n) {
        return String.format(Locale.GERMANY, "%,d", n);
    }
}
