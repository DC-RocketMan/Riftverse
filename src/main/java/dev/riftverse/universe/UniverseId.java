package dev.riftverse.universe;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Adresse eines Universums: Welche der vier Rift-Dimensionen (= Himmelstyp) und
 * welche Zelle im Raster. Jede Zelle ist ein eigenes Universum mit eigener Physik.
 */
public record UniverseId(int variant, int cellX, int cellZ) {

    private static final String LETTERS = "ABCDEFGHJKLMNPRSTVWXZ";

    /** Deterministischer 64-Bit-Seed dieses Universums. */
    public long seed() {
        long h = 0x9E3779B97F4A7C15L;
        h = mix(h ^ (variant * 0xBF58476D1CE4E5B9L));
        h = mix(h ^ (cellX * 0x94D049BB133111EBL));
        h = mix(h ^ (cellZ * 0xD6E8FEB86659FD93L));
        return h;
    }

    /** Kurzcode im Stil „Universum K-412“. */
    public String code() {
        long s = seed();
        char letter = LETTERS.charAt((int) Math.floorMod(s, LETTERS.length()));
        int number = (int) Math.floorMod(s >>> 17, 900) + 100;
        return letter + "-" + number;
    }

    /** Schlüssel für Speicherdaten. */
    public String key() {
        return variant + ":" + cellX + ":" + cellZ;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("V", variant);
        tag.putInt("X", cellX);
        tag.putInt("Z", cellZ);
        return tag;
    }

    @Nullable
    public static UniverseId load(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains("V")) return null;
        return new UniverseId(tag.getInt("V"), tag.getInt("X"), tag.getInt("Z"));
    }

    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
