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

    /** Wie oft eine Universumsprobe schon im Universums-Destillator lief. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VERSCHLEISS = COMPONENTS.register("verschleiss",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    /** Erster Knoten, den eine Kopplungskarte sich gemerkt hat. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> KOPPLUNG = COMPONENTS.register("kopplung",
            () -> DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

    /** Gespeicherte Ziele der Portal-Kanone Mk III. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<KanonenZiele>> KANONEN_ZIELE = COMPONENTS.register("kanonen_ziele",
            () -> DataComponentType.<KanonenZiele>builder().persistent(KanonenZiele.CODEC).networkSynchronized(KanonenZiele.STREAM_CODEC).build());

    /**
     * Bis zu 8 Universen, die eine Kanone ab Mk III kennt, und was gerade gewählt ist.
     *
     * @param auswahl {@link #ZUFALL}, {@link #KURZSTRECKE} oder der Index eines Ziels
     */
    public record KanonenZiele(java.util.List<UniverseId> ziele, int auswahl) {
        public static final int ZUFALL = -1, KURZSTRECKE = -2, MAX = 8;
        public static final KanonenZiele LEER = new KanonenZiele(java.util.List.of(), ZUFALL);
        public static final Codec<KanonenZiele> CODEC = RecordCodecBuilder.create(i -> i.group(
                UniverseId.CODEC.listOf().fieldOf("ziele").forGetter(KanonenZiele::ziele),
                Codec.INT.optionalFieldOf("auswahl", ZUFALL).forGetter(KanonenZiele::auswahl)
        ).apply(i, KanonenZiele::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, KanonenZiele> STREAM_CODEC = StreamCodec.composite(
                UniverseId.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), KanonenZiele::ziele,
                ByteBufCodecs.VAR_INT, KanonenZiele::auswahl,
                KanonenZiele::new);

        public KanonenZiele {
            ziele = java.util.List.copyOf(ziele);
        }

        /** Nächste Auswahl: Zufall → Kurzstrecke → Ziel 1 … n → Zufall. */
        public KanonenZiele weiter() {
            int next;
            if (auswahl == ZUFALL) next = KURZSTRECKE;
            else if (auswahl == KURZSTRECKE) next = ziele.isEmpty() ? ZUFALL : 0;
            else next = auswahl + 1 < ziele.size() ? auswahl + 1 : ZUFALL;
            return new KanonenZiele(ziele, next);
        }

        /** Merkt sich ein Universum (das älteste fliegt raus, wenn alle 8 Plätze belegt sind) und wählt es aus. */
        public KanonenZiele merken(UniverseId id) {
            java.util.List<UniverseId> list = new java.util.ArrayList<>(ziele);
            int idx = list.indexOf(id);
            if (idx < 0) {
                if (list.size() >= MAX) list.remove(0);
                list.add(id);
                idx = list.size() - 1;
            }
            return new KanonenZiele(list, idx);
        }

        /** Löscht das gewählte Ziel. */
        public KanonenZiele loeschen() {
            if (auswahl < 0 || auswahl >= ziele.size()) return this;
            java.util.List<UniverseId> list = new java.util.ArrayList<>(ziele);
            list.remove(auswahl);
            return new KanonenZiele(list, ZUFALL);
        }

        @org.jetbrains.annotations.Nullable
        public UniverseId gewaehlt() {
            return auswahl >= 0 && auswahl < ziele.size() ? ziele.get(auswahl) : null;
        }
    }

    /**
     * @param mb      Portalfluid in mB
     * @param zerfall Zerfallszeitpunkt (0 = zerfällt nicht)
     * @param stabil  stabilisiertes Portalfluid aus der Raffinerie statt Roh-Portalfluid
     * @param konzentrat Portalfluid-Konzentrat (Meilenstein 4); dann ist auch {@code stabil} gesetzt
     */
    public record KanonenTank(int mb, long zerfall, boolean stabil, boolean konzentrat) {
        public static final KanonenTank LEER = new KanonenTank(0, 0, false, false);
        public static final Codec<KanonenTank> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("mb").forGetter(KanonenTank::mb),
                Codec.LONG.fieldOf("zerfall").forGetter(KanonenTank::zerfall),
                Codec.BOOL.optionalFieldOf("stabil", false).forGetter(KanonenTank::stabil),
                Codec.BOOL.optionalFieldOf("konzentrat", false).forGetter(KanonenTank::konzentrat)
        ).apply(i, KanonenTank::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, KanonenTank> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, KanonenTank::mb,
                ByteBufCodecs.VAR_LONG, KanonenTank::zerfall,
                ByteBufCodecs.BOOL, KanonenTank::stabil,
                ByteBufCodecs.BOOL, KanonenTank::konzentrat,
                KanonenTank::new);

        public KanonenTank(int mb, long zerfall) {
            this(mb, zerfall, false, false);
        }

        public KanonenTank(int mb, long zerfall, boolean stabil) {
            this(mb, zerfall, stabil, false);
        }

        /** Fluid-Stufe: 1 roh, 2 stabil, 3 Konzentrat. */
        public int stufe() {
            return konzentrat ? 3 : stabil ? 2 : 1;
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
