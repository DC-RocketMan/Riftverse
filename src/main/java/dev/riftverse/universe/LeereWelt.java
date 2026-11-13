package dev.riftverse.universe;

import dev.riftverse.Riftverse;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Die Leere Welt frisst Licht: Fackeln, Laternen, Leuchtstein und alles andere, was ein Spieler dort an Licht
 * setzt, erlischt nach {@link #FRIST} Ticks und verschwindet ohne Drop. Ausgenommen sind Riftverse-Blöcke (Maschinen
 * leuchten nur, solange sie arbeiten). Betten explodieren dort (dimension_type: bed_works = false), ein Klon-Tank
 * lässt sich gar nicht erst aufstellen – einen Respawn-Anker gibt es in der Leere nicht.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class LeereWelt {
    public static final int FRIST = 100;

    private record Licht(ServerLevel level, BlockPos pos, long due) {}

    private static final List<Licht> PENDING = new ArrayList<>();

    public static boolean isLeere(Level level) {
        return UniverseType.of(level.dimension()) == UniverseType.LEERE;
    }

    /** Wird dieses Licht von der Leere gefressen? */
    public static boolean frisst(BlockState state) {
        if (state.getLightEmission() <= 0) return false;
        return !Riftverse.MODID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isLeere(level)) return;
        if (!frisst(event.getPlacedBlock())) return;
        merken(level, event.getPos());
        if (event.getEntity() instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.riftverse.leere.licht").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }

    /** Merkt ein frisch gesetztes Licht vor; nach {@link #FRIST} Ticks frisst die Leere es. */
    public static void merken(ServerLevel level, BlockPos pos) {
        PENDING.add(new Licht(level, pos.immutable(), level.getGameTime() + FRIST));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || PENDING.isEmpty() || !isLeere(level)) return;
        long now = level.getGameTime();
        Iterator<Licht> it = PENDING.iterator();
        while (it.hasNext()) {
            Licht l = it.next();
            if (l.level() != level) continue;
            if (now < l.due()) {
                if (now % 10 == 0) {
                    level.sendParticles(ParticleTypes.SQUID_INK, l.pos().getX() + 0.5, l.pos().getY() + 0.5, l.pos().getZ() + 0.5,
                            2, 0.25, 0.25, 0.25, 0.0);
                }
                continue;
            }
            it.remove();
            if (!level.isLoaded(l.pos()) || !frisst(level.getBlockState(l.pos()))) continue;
            level.destroyBlock(l.pos(), false);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, l.pos().getX() + 0.5, l.pos().getY() + 0.5, l.pos().getZ() + 0.5,
                    12, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, l.pos(), SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 0.8f, 0.5f);
        }
    }

    /** Zählt für den Rauchtest, wie viele Lichter gerade auf ihr Ende warten. */
    public static int pending() {
        return PENDING.size();
    }

    private LeereWelt() {}
}
