package dev.riftverse.story;

import dev.riftverse.Config;
import dev.riftverse.Riftverse;
import dev.riftverse.boss.RiftBossEntity;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModEntities;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.MultiverseData;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Regie: erkennt, wann ein Spieler ein Universum betritt, und zeigt Typ, Name und Modifikatoren.
 * Die Boss-Handlung aus dem ersten Entwurf ist bis zum Story-Modus (Phase 3) abgeschaltet und lässt sich
 * zum Testen über {@code story.boss_bei_ankunft} wieder einschalten.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class StoryDirector {
    private static final Map<UUID, UniverseId> LAST_SEEN = new HashMap<>();
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private record Pending(UniverseId universe, int startTick) {}

    // ------------------------------------------------------------------ Ankunft erkennen

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        UniverseId now = Multiverse.at(player.level(), player.blockPosition());
        UniverseId before = LAST_SEEN.get(player.getUUID());
        if (Objects.equals(now, before)) return;
        if (now == null) {
            LAST_SEEN.remove(player.getUUID());
            return;
        }
        LAST_SEEN.put(player.getUUID(), now);
        onArrive(player, now);
    }

    private static void onArrive(ServerPlayer player, UniverseId id) {
        UniverseProfile u = UniverseProfile.get(id);
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(u.type.displayName().copy().withStyle(ChatFormatting.GREEN)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(u.title() + " „" + u.name + "“")
                .withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.literal("⌁ ").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable("message.riftverse.arrival", u.type.displayName(), u.modifierText()))
                .withStyle(ChatFormatting.DARK_AQUA));

        MultiverseData data = MultiverseData.get(player.server);
        MultiverseData.Record rec = data.record(id);
        rec.visits++;
        data.setDirty();

        // Universen sind beim Betreten friedlich. Der Boss-Start aus dem ersten Entwurf bleibt bis zum
        // Story-Modus abgeschaltet (Config story.boss_bei_ankunft).
        if (!Config.BOSS_ON_ARRIVAL.get()) return;
        switch (rec.state) {
            case MultiverseData.STATE_NONE -> {
                PENDING.put(player.getUUID(), new Pending(id, player.server.getTickCount() + 120));
                player.sendSystemMessage(Component.literal("⌁ Dein Rift-Empfänger knistert … ein schwaches Signal.")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            case MultiverseData.STATE_ACTIVE -> {
                String where = rec.arena != null
                        ? " bei " + rec.arena.getX() + " / " + rec.arena.getY() + " / " + rec.arena.getZ() : "";
                player.sendSystemMessage(Component.literal("⚠ " + rec.bossName + " herrscht hier noch" + where + ".")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            case MultiverseData.STATE_DEFEATED, MultiverseData.STATE_SPARED ->
                    player.sendSystemMessage(Component.literal("✦ Die Bewohner erkennen dich wieder: „" + rec.epithet + "“!")
                            .withStyle(ChatFormatting.GOLD));
            default -> { }
        }
    }

    // ------------------------------------------------------------------ Handlung starten

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Pending> e = it.next();
            if (tick < e.getValue().startTick()) continue;
            it.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            if (player == null) continue;
            UniverseId here = Multiverse.at(player.level(), player.blockPosition());
            if (!e.getValue().universe().equals(here)) continue;
            MultiverseData data = MultiverseData.get(server);
            if (data.record(here).state != MultiverseData.STATE_NONE) continue;
            try {
                startPlot(player, here, data);
            } catch (Exception ex) {
                Riftverse.LOGGER.error("Konnte Handlung nicht starten", ex);
            }
        }
    }

    private static void startPlot(ServerPlayer player, UniverseId id, MultiverseData data) {
        ServerLevel level = player.serverLevel();
        UniverseProfile u = UniverseProfile.get(id);
        RandomSource r = level.getRandom();
        BossPlot plot = StoryEngine.createPlot(r, u);

        // Arena 40–70 Blöcke entfernt, aber sicher innerhalb dieses Universums
        double angle = r.nextDouble() * Math.PI * 2;
        int dist = 40 + r.nextInt(31);
        int cellMinX = id.cellX() * Multiverse.CELL, cellMinZ = id.cellZ() * Multiverse.CELL;
        int margin = Multiverse.BORDER + 24;
        int ax = Mth.clamp(player.getBlockX() + (int) (Math.sin(angle) * dist), cellMinX + margin, cellMinX + Multiverse.CELL - margin);
        int az = Mth.clamp(player.getBlockZ() - (int) (Math.cos(angle) * dist), cellMinZ + margin, cellMinZ + Multiverse.CELL - margin);

        level.getChunk(ax >> 4, az >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ax, az);
        if (y <= level.getMinBuildHeight() + 2) y = player.getBlockY();
        y = Math.min(y, level.getMaxBuildHeight() - 12);
        BlockPos arena = new BlockPos(ax, y, az);
        buildArena(level, arena);

        int liberated = Math.min(data.liberated(), 12);
        double health = 120 + 25 * liberated + u.danger * 20;
        double damage = 5 + liberated * 0.6 + u.danger;

        RiftBossEntity boss = new RiftBossEntity(ModEntities.RIFT_BOSS.get(), level);
        boss.moveTo(ax + 0.5, y + 1, az + 0.5, r.nextFloat() * 360f, 0f);
        boss.setup(plot, id, health, damage, arena.above());
        level.addFreshEntity(boss);

        MultiverseData.Record rec = data.record(id);
        rec.state = MultiverseData.STATE_ACTIVE;
        rec.bossName = plot.fullName();
        rec.bossId = boss.getUUID();
        rec.arena = arena.above();
        data.setDirty();

        int realDist = (int) Math.round(Math.sqrt(player.distanceToSqr(ax + 0.5, y, az + 0.5)));
        String dir = direction(ax - player.getX(), az - player.getZ());
        List<Component> hook = StoryEngine.hookMessages(plot, u, player.getName().getString(), realDist, dir, r);
        for (ServerPlayer p : level.players()) {
            if (!id.equals(Multiverse.at(level, p.blockPosition()))) continue;
            hook.forEach(p::sendSystemMessage);
            p.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.6f);
        }
    }

    /** Runde Plattform mit Säulen – der Ort des Showdowns. */
    private static void buildArena(ServerLevel level, BlockPos center) {
        BlockState floor = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState support = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState ring = ModBlocks.RIFT_STONE.get().defaultBlockState();
        BlockState pillar = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        int radius = 8;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > radius * radius) continue;
                BlockPos f = center.offset(dx, 0, dz);
                level.setBlock(f, d2 >= (radius - 1) * (radius - 1) ? ring : floor, 3);
                for (int dy = 1; dy <= 7; dy++) {
                    BlockPos p = f.above(dy);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, air, 3);
                }
                for (int dy = 1; dy <= 4; dy++) {
                    BlockPos p = f.below(dy);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || !s.getFluidState().isEmpty()) level.setBlock(p, support, 3);
                    else break;
                }
            }
        }
        int[][] spots = {{6, 0}, {-6, 0}, {0, 6}, {0, -6}};
        for (int[] s : spots) {
            for (int h = 1; h <= 4; h++) level.setBlock(center.offset(s[0], h, s[1]), pillar, 3);
            level.setBlock(center.offset(s[0], 5, s[1]), Blocks.END_ROD.defaultBlockState(), 3);
        }
    }

    private static String direction(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        angle = (angle % 360 + 360) % 360;
        String[] dirs = {"nördlich", "nordöstlich", "östlich", "südöstlich", "südlich", "südwestlich", "westlich", "nordwestlich"};
        return dirs[(int) Math.round(angle / 45.0) % 8];
    }

    // ------------------------------------------------------------------ Sonstige Ereignisse

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().getEntity() instanceof RiftBossEntity boss) {
            boss.onKilledPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SEEN.remove(event.getEntity().getUUID());
        PENDING.remove(event.getEntity().getUUID());
    }

    private StoryDirector() {}
}
