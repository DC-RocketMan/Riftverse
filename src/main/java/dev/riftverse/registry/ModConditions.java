package dev.riftverse.registry;

import com.mojang.serialization.MapCodec;
import dev.riftverse.Riftverse;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Datapack-Bedingungen ({@code "neoforge:conditions"}) des Mods. */
public final class ModConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Riftverse.MODID);

    static {
        CONDITIONS.register("entwicklung", () -> Entwicklung.CODEC);
    }

    private ModConditions() {}

    /**
     * Nur im Entwicklungsmodus: Rauchtest ({@code -Driftverse.rauchtest=true}), Start aus der Entwicklungsumgebung oder
     * die Config-Option {@code entwicklung.testwelten}.
     */
    public static boolean entwicklung() {
        if (Boolean.getBoolean("riftverse.rauchtest") || !FMLEnvironment.production) return true;
        try {
            return dev.riftverse.Config.TESTWELTEN.get();
        } catch (IllegalStateException e) {
            return false;   // Config noch nicht geladen
        }
    }

    /** {@code {"type": "riftverse:entwicklung"}}: wahr nur im Entwicklungsmodus (Testwelten). */
    public static final class Entwicklung implements ICondition {
        public static final Entwicklung INSTANCE = new Entwicklung();
        public static final MapCodec<Entwicklung> CODEC = MapCodec.unit(INSTANCE).stable();

        private Entwicklung() {}

        @Override
        public boolean test(IContext context) {
            return entwicklung();
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }

        @Override
        public String toString() {
            return "riftverse:entwicklung";
        }
    }
}
