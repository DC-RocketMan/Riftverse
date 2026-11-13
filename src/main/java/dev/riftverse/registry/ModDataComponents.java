package dev.riftverse.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riftverse.Riftverse;
import dev.riftverse.universe.UniverseId;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Riftverse.MODID);

    /** Gespeicherte FE in Items (Energiezelle, Xenit-Zelle als Item). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = COMPONENTS.register("energy",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    /** Zerfallszeitpunkt von Roh-Portalfluid (an Kartuschen und an FluidStacks). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> ZERFALL = COMPONENTS.register("zerfall",
            () -> DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

    /** Füllung der Portal-Kanone. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<KanonenTank>> KANONEN_TANK = COMPONENTS.register("kanonen_tank",
            () -> DataComponentType.<KanonenTank>builder().persistent(KanonenTank.CODEC).networkSynchronized(KanonenTank.STREAM_CODEC).build());

    /** Ziel des Meteoritenpeilers. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PeilerZiel>> PEILER = COMPONENTS.register("peiler",
            () -> DataComponentType.<PeilerZiel>builder().persistent(PeilerZiel.CODEC).networkSynchronized(PeilerZiel.STREAM_CODEC).build());

    /** Universum einer Universumsprobe oder Koordinatenkarte. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UniverseId>> ZIEL = COMPONENTS.register("ziel",
            () -> DataComponentType.<UniverseId>builder().persistent(UniverseId.CODEC).networkSynchronized(UniverseId.STREAM_CODEC).build());

    /**
     * @param mb      Portalfluid in mB
     * @param zerfall Zerfallszeitpunkt (0 = zerfällt nicht)
     * @param stabil  stabilisiertes Portalfluid aus der Raffinerie statt Roh-Portalfluid
     */
    public record KanonenTank(int mb, long zerfall, boolean stabil) {
        public static final KanonenTank LEER = new KanonenTank(0, 0, false);
        public static final Codec<KanonenTank> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("mb").forGetter(KanonenTank::mb),
                Codec.LONG.fieldOf("zerfall").forGetter(KanonenTank::zerfall),
                Codec.BOOL.optionalFieldOf("stabil", false).forGetter(KanonenTank::stabil)
        ).apply(i, KanonenTank::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, KanonenTank> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, KanonenTank::mb,
                ByteBufCodecs.VAR_LONG, KanonenTank::zerfall,
                ByteBufCodecs.BOOL, KanonenTank::stabil,
                KanonenTank::new);

        public KanonenTank(int mb, long zerfall) {
            this(mb, zerfall, false);
        }
    }

    /** @param zone true, solange der Träger in der Xenit-Zone (200 Blöcke) ist */
    public record PeilerZiel(GlobalPos pos, boolean zone) {
        public static final Codec<PeilerZiel> CODEC = RecordCodecBuilder.create(i -> i.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(PeilerZiel::pos),
                Codec.BOOL.fieldOf("zone").forGetter(PeilerZiel::zone)
        ).apply(i, PeilerZiel::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, PeilerZiel> STREAM_CODEC = StreamCodec.composite(
                GlobalPos.STREAM_CODEC, PeilerZiel::pos,
                ByteBufCodecs.BOOL, PeilerZiel::zone,
                PeilerZiel::new);
    }

    private ModDataComponents() {}
}
