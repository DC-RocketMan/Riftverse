package dev.riftverse.gadget;

import dev.riftverse.Riftverse;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Helfer-Konstrukte der Aufgaben-Box: ein Eisengolem mit Würfelkopf-Display (der Name zeigt die Aufgabe als
 * Symbol). Er lebt 5 Minuten, die letzte Viertelminute ist er instabil (rot, raucht), dann zerfällt er in einer
 * Explosion, die keine Blöcke zerstört. Helfer lassen nichts fallen.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class Helfer {
    public static final String KEY = "riftverse:helfer";
    public static final int LEBEN = 6_000, INSTABIL = 300, ADER_MAX = 64, ERNTE_RADIUS = 8;

    public enum Aufgabe { ERZ, ERNTE, KAMPF }

    public static boolean erzAn(BlockState s) {
        return s.is(Tags.Blocks.ORES);
    }

    public static boolean feldAn(BlockState s) {
        return s.getBlock() instanceof CropBlock || s.getBlock() instanceof FarmBlock;
    }

    @Nullable
    public static IronGolem beschwoeren(ServerLevel level, Player owner, Aufgabe a, BlockPos ziel, BlockPos spawn) {
        IronGolem g = EntityType.IRON_GOLEM.create(level);
        if (g == null) return null;
        g.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, owner.getYRot() + 180f, 0f);
        g.setPlayerCreated(true);
        g.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), MobSpawnType.MOB_SUMMONED, null);
        CompoundTag t = new CompoundTag();
        t.putString("Aufgabe", a.name());
        t.putLong("Ziel", ziel.asLong());
        t.putLong("Ende", level.getGameTime() + LEBEN);
        t.putUUID("Besitzer", owner.getUUID());
        t.putString("Block", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(ziel).getBlock()).toString());
        g.getPersistentData().put(KEY, t);
        g.setCustomName(name(a, false));
        g.setCustomNameVisible(true);
        g.setPersistenceRequired();
        if (a != Aufgabe.KAMPF) g.setNoAi(true);
        level.addFreshEntity(g);
        level.playSound(null, spawn, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 1f, 1.4f);
        level.sendParticles(ParticleTypes.CLOUD, g.getX(), g.getY() + 1, g.getZ(), 20, 0.4, 0.8, 0.4, 0.02);
        return g;
    }

    private static Component name(Aufgabe a, boolean instabil) {
        String sym = switch (a) {
            case ERZ -> "⛏";
            case ERNTE -> "✿";
            case KAMPF -> "⚔";
        };
        return Component.literal("[" + sym + "] ").append(Component.translatable("entity.riftverse.helfer." + a.name().toLowerCase()))
                .withStyle(instabil ? ChatFormatting.RED : ChatFormatting.GREEN);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof IronGolem g) || !(g.level() instanceof ServerLevel sl)) return;
        if (g.tickCount % 10 != 0 || !g.getPersistentData().contains(KEY)) return;
        CompoundTag t = g.getPersistentData().getCompound(KEY);
        Aufgabe a;
        try {
            a = Aufgabe.valueOf(t.getString("Aufgabe"));
        } catch (IllegalArgumentException e) {
            return;
        }
        long rest = t.getLong("Ende") - sl.getGameTime();
        if (rest <= 0) {
            zerfallen(sl, g);
            return;
        }
        if (rest <= INSTABIL) {
            if (!t.getBoolean("Instabil")) {
                t.putBoolean("Instabil", true);
                g.setCustomName(name(a, true));
            }
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, g.getX(), g.getY() + 2.6, g.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
        }
        switch (a) {
            case ERZ -> ader(sl, g, t);
            case ERNTE -> ernte(sl, g, t);
            case KAMPF -> kaempfen(sl, g, t);
        }
    }

    /** Baut einen Block der Ader ab (alle 10 Ticks), immer den nächsten zusammenhängenden. */
    private static void ader(ServerLevel level, IronGolem g, CompoundTag t) {
        if (t.getBoolean("Fertig")) return;
        BlockPos start = BlockPos.of(t.getLong("Ziel"));
        String block = t.getString("Block");
        long[] weg = t.getLongArray("Weg");
        if (weg.length >= ADER_MAX) {
            t.putBoolean("Fertig", true);
            return;
        }
        BlockPos next = naechster(level, start, weg, block);
        if (next == null) {
            t.putBoolean("Fertig", true);
            return;
        }
        g.moveTo(next.getX() + 0.5, next.getY(), next.getZ() + 0.5);
        g.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        level.destroyBlock(next, true, g);
        long[] neu = java.util.Arrays.copyOf(weg, weg.length + 1);
        neu[weg.length] = next.asLong();
        t.putLongArray("Weg", neu);
    }

    /** Nächster Block der Ader: Suche vom Startblock und allen schon abgebauten Blöcken aus. */
    @Nullable
    private static BlockPos naechster(Level level, BlockPos start, long[] weg, String block) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        for (long w : weg) {
            BlockPos p = BlockPos.of(w);
            if (seen.add(p)) queue.add(p);
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            if (gleich(level, p, block)) return p;
            for (Direction d : Direction.values()) {
                BlockPos q = p.relative(d);
                if (seen.add(q) && gleich(level, q, block)) return q;
            }
        }
        return null;
    }

    private static boolean gleich(Level level, BlockPos p, String block) {
        BlockState s = level.getBlockState(p);
        return !s.isAir() && net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString().equals(block);
    }

    /** Erntet eine reife Pflanze im Umkreis und sät sie neu. */
    private static void ernte(ServerLevel level, IronGolem g, CompoundTag t) {
        BlockPos c = BlockPos.of(t.getLong("Ziel"));
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-ERNTE_RADIUS, -1, -ERNTE_RADIUS), c.offset(ERNTE_RADIUS, 2, ERNTE_RADIUS))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s)) {
                BlockPos at = p.immutable();
                g.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                level.destroyBlock(at, true, g);
                level.setBlockAndUpdate(at, crop.getStateForAge(0));
                t.putInt("Geerntet", t.getInt("Geerntet") + 1);
                return;
            }
        }
    }

    /** Greift den nächsten Feind an, der dem Besitzer zu nahe kommt. */
    private static void kaempfen(ServerLevel level, IronGolem g, CompoundTag t) {
        if (g.getTarget() != null && g.getTarget().isAlive()) return;
        Player owner = level.getPlayerByUUID(t.getUUID("Besitzer"));
        LivingEntity center = owner != null && owner.distanceToSqr(g) < 32 * 32 ? owner : g;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(16), e -> e instanceof Enemy && e.isAlive())) {
            g.setTarget(e);
            return;
        }
    }

    private static void zerfallen(ServerLevel level, IronGolem g) {
        level.explode(g, g.getX(), g.getY() + 1, g.getZ(), 2.0f, Level.ExplosionInteraction.NONE);
        level.sendParticles(ParticleTypes.EXPLOSION, g.getX(), g.getY() + 1, g.getZ(), 4, 0.5, 0.5, 0.5, 0);
        g.discard();
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().contains(KEY)) event.setCanceled(true);
    }


    private Helfer() {}
}
