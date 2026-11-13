package dev.riftverse.tech;

import dev.riftverse.Riftverse;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Welcher Spieler welchen Klon-Tank hat, und was beim Tod passiert:
 * <ol>
 *   <li>Stirbt ein Spieler mit fertigem Klon, wird sein Inventar beiseitegelegt statt fallen gelassen.</li>
 *   <li>Beim Respawn bekommt er alles zurück und wacht vor seinem Klon-Tank auf; der Tank ist danach leer.</li>
 * </ol>
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class Klone extends SavedData {
    private static final String NAME = "riftverse_klone";
    public static final String INVENTAR = "riftverse:klon_inventar";
    public static final String ZIEL = "riftverse:klon_ziel";

    private final Map<UUID, GlobalPos> tanks = new HashMap<>();

    public static Klone get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Klone::new, Klone::load, null), NAME);
    }

    public void set(UUID player, GlobalPos pos) {
        tanks.put(player, pos);
        setDirty();
    }

    public void remove(UUID player, GlobalPos pos) {
        if (pos.equals(tanks.get(player))) {
            tanks.remove(player);
            setDirty();
        }
    }

    @Nullable
    public GlobalPos tank(UUID player) {
        return tanks.get(player);
    }

    /** Fertiger Klon-Tank des Spielers, oder null (lädt den Chunk des Tanks bei Bedarf). */
    @Nullable
    public static KlonTankBlockEntity bereiterTank(MinecraftServer server, UUID player) {
        GlobalPos pos = get(server).tank(player);
        if (pos == null) return null;
        ServerLevel level = server.getLevel(pos.dimension());
        if (level == null) return null;
        if (level.getBlockEntity(pos.pos()) instanceof KlonTankBlockEntity tank && player.equals(tank.owner()) && tank.bereit()) {
            return tank;
        }
        return null;
    }

    // ------------------------------------------------------------------ Tod und Respawn

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        KlonTankBlockEntity tank = bereiterTank(server, player.getUUID());
        if (tank == null || tank.getLevel() == null) return;

        tank.verbrauchen();
        CompoundTag data = player.getPersistentData();
        if (!player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            ListTag inv = player.getInventory().save(new ListTag());
            data.put(INVENTAR, inv);
            player.getInventory().clearContent();
        }
        CompoundTag ziel = new CompoundTag();
        ziel.putString("Dim", tank.getLevel().dimension().location().toString());
        ziel.putLong("Pos", tank.getBlockPos().asLong());
        data.put(ZIEL, ziel);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag old = event.getOriginal().getPersistentData();
        if (old.contains(INVENTAR, Tag.TAG_LIST)) {
            player.getInventory().load(old.getList(INVENTAR, Tag.TAG_COMPOUND));
            old.remove(INVENTAR);
        }
        if (old.contains(ZIEL, Tag.TAG_COMPOUND)) {
            player.getPersistentData().put(ZIEL, old.getCompound(ZIEL).copy());
            old.remove(ZIEL);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() || !(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ZIEL, Tag.TAG_COMPOUND)) return;
        CompoundTag ziel = data.getCompound(ZIEL);
        data.remove(ZIEL);
        ResourceLocation dim = ResourceLocation.tryParse(ziel.getString("Dim"));
        MinecraftServer server = player.getServer();
        if (dim == null || server == null) return;
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        if (level == null) return;
        BlockPos tank = BlockPos.of(ziel.getLong("Pos"));
        BlockState state = level.getBlockState(tank);
        Direction front = state.hasProperty(TechBlock.FACING) ? state.getValue(TechBlock.FACING) : Direction.NORTH;
        BlockPos spot = tank.relative(front);
        float yaw = front.getOpposite().toYRot();
        player.teleportTo(level, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0f);
        player.displayClientMessage(Component.translatable("message.riftverse.klon.aufgewacht").withStyle(ChatFormatting.GREEN), false);
    }

    // ------------------------------------------------------------------ Speichern

    private static Klone load(CompoundTag tag, HolderLookup.Provider registries) {
        Klone k = new Klone();
        CompoundTag t = tag.getCompound("Tanks");
        for (String key : t.getAllKeys()) {
            try {
                GlobalPos.CODEC.parse(NbtOps.INSTANCE, t.get(key)).result().ifPresent(p -> k.tanks.put(UUID.fromString(key), p));
            } catch (IllegalArgumentException ignored) {
                // kaputter Eintrag
            }
        }
        return k;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        tanks.forEach((uuid, pos) -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().ifPresent(n -> t.put(uuid.toString(), n)));
        tag.put("Tanks", t);
        return tag;
    }

    /** Für den Rauchtest: Ziel eines Spielers nach dem Tod. */
    public static boolean hatRespawnZiel(ServerPlayer player) {
        return player.getPersistentData().contains(ZIEL, Tag.TAG_COMPOUND);
    }
}
