package dev.riftverse.portal;

import dev.riftverse.registry.ModAttachments;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Der Heimanker eines Spielers: der Ort, von dem aus er zuletzt aus seiner Heimat-Realität (nicht aus einem
 * Universum) gesprungen ist. Ohne Anker führt die Rückkehr zum Weltspawn.
 */
public record Heimanker(ResourceKey<Level> dimension, Vec3 pos) {

    public static Heimanker of(ServerPlayer player) {
        CompoundTag tag = player.getData(ModAttachments.HEIMANKER);
        if (tag.contains("Dim")) {
            ResourceLocation dim = ResourceLocation.tryParse(tag.getString("Dim"));
            if (dim != null) {
                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dim);
                if (player.server.getLevel(key) != null) {
                    return new Heimanker(key, new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")));
                }
            }
        }
        return new Heimanker(Level.OVERWORLD, Vec3.atBottomCenterOf(player.server.overworld().getSharedSpawnPos()));
    }

    public static void set(ServerPlayer player, ResourceKey<Level> dimension, Vec3 pos) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dim", dimension.location().toString());
        tag.putDouble("X", pos.x);
        tag.putDouble("Y", pos.y);
        tag.putDouble("Z", pos.z);
        player.setData(ModAttachments.HEIMANKER, tag);
    }
}
