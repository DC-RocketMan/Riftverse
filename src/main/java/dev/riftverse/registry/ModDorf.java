package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.dorf.DekoLichtBlock;
import dev.riftverse.dorf.HaengendBlock;
import dev.riftverse.dorf.LeuchtLogik;
import dev.riftverse.dorf.LeuchtpflanzeBlock;
import dev.riftverse.dorf.LeuchtpflanzeBlock.Art;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Blöcke, Items und Effekte von Bridge's Inn (Meilenstein 10 der neuen Universen). Sie hängen an den Registern von
 * {@link ModBlocks} und {@link ModItems}; {@link #init()} lädt die Klasse rechtzeitig.
 */
public final class ModDorf {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Riftverse.MODID);

    // ------------------------------------------------------------------ Leuchtpflanzen

    /** Name → Block; die Reihenfolge ist auch die im Handbuch. */
    public static final Map<String, DeferredBlock<LeuchtpflanzeBlock>> PFLANZEN = new LinkedHashMap<>();

    public static final DeferredBlock<LeuchtpflanzeBlock> STERNENBLUME_WEISS = pflanze("sternenblume_weiss", Art.STERNENBLUME, 0xF4F0FF);
    public static final DeferredBlock<LeuchtpflanzeBlock> STERNENBLUME_GOLD = pflanze("sternenblume_gold", Art.STERNENBLUME, 0xFFD45A);
    public static final DeferredBlock<LeuchtpflanzeBlock> LEUCHTGLOCKE_BLAU = pflanze("leuchtglocke_blau", Art.LEUCHTGLOCKE, 0x4A8CFF);
    public static final DeferredBlock<LeuchtpflanzeBlock> LEUCHTGLOCKE_VIOLETT = pflanze("leuchtglocke_violett", Art.LEUCHTGLOCKE, 0xB06BFF);
    public static final DeferredBlock<LeuchtpflanzeBlock> LEUCHTGLOCKE_TUERKIS = pflanze("leuchtglocke_tuerkis", Art.LEUCHTGLOCKE, 0x3FF0D8);
    public static final DeferredBlock<LeuchtpflanzeBlock> LEUCHTGLOCKE_ROSA = pflanze("leuchtglocke_rosa", Art.LEUCHTGLOCKE, 0xFF7FC8);
    public static final DeferredBlock<LeuchtpflanzeBlock> MONDKRAUT = pflanze("mondkraut", Art.MONDKRAUT, 0xC8E0FF);
    public static final DeferredBlock<LeuchtpflanzeBlock> FUNKENMOHN_ROT = pflanze("funkenmohn_rot", Art.FUNKENMOHN, 0xFF4A3A);
    public static final DeferredBlock<LeuchtpflanzeBlock> FUNKENMOHN_ORANGE = pflanze("funkenmohn_orange", Art.FUNKENMOHN, 0xFF9A2A);
    public static final DeferredBlock<LeuchtpflanzeBlock> ZAUBERPILZ_VIOLETT = pflanze("zauberpilz_violett", Art.ZAUBERPILZ, 0xC05AFF);
    public static final DeferredBlock<LeuchtpflanzeBlock> ZAUBERPILZ_GRUEN = pflanze("zauberpilz_gruen", Art.ZAUBERPILZ, 0x6AFF7A);

    public static final DeferredBlock<HaengendBlock> LICHTRANKE = ModBlocks.BLOCKS.register("lichtranke",
            () -> new HaengendBlock(0x9AF0FF, pflanzenProps(MapColor.COLOR_CYAN)
                    .lightLevel(s -> LeuchtLogik.licht(s, 9, 0))));
    public static final DeferredBlock<HaengendBlock> KRAEUTERBUENDEL = ModBlocks.BLOCKS.register("kraeuterbuendel",
            () -> new HaengendBlock(0, pflanzenProps(MapColor.PLANT)));

    // ------------------------------------------------------------------ Deko

    public static final DeferredBlock<Block> DACHZIEGEL = ModBlocks.BLOCKS.register("violette_dachziegel",
            () -> new Block(steinProps(MapColor.COLOR_PURPLE)));
    public static final DeferredBlock<StairBlock> DACHZIEGEL_TREPPE = ModBlocks.BLOCKS.register("violette_dachziegel_treppe",
            () -> new StairBlock(DACHZIEGEL.get().defaultBlockState(), steinProps(MapColor.COLOR_PURPLE)));
    public static final DeferredBlock<SlabBlock> DACHZIEGEL_STUFE = ModBlocks.BLOCKS.register("violette_dachziegel_stufe",
            () -> new SlabBlock(steinProps(MapColor.COLOR_PURPLE)));
    public static final DeferredBlock<Block> FACHWERK = ModBlocks.BLOCKS.register("fachwerk",
            () -> new Block(holzProps(MapColor.TERRACOTTA_WHITE)));
    public static final DeferredBlock<Block> FACHWERK_KREUZ = ModBlocks.BLOCKS.register("fachwerk_kreuz",
            () -> new Block(holzProps(MapColor.TERRACOTTA_WHITE)));
    public static final DeferredBlock<Block> BUNTES_KOPFSTEINPFLASTER = ModBlocks.BLOCKS.register("buntes_kopfsteinpflaster",
            () -> new Block(steinProps(MapColor.STONE)));
    public static final DeferredBlock<TransparentBlock> STERNGLAS = ModBlocks.BLOCKS.register("sternglas",
            () -> new TransparentBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.3f)
                    .sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 3)
                    .isValidSpawn((s, l, p, t) -> false).isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
    public static final DeferredBlock<RotatedPillarBlock> FUNKELHOLZ_STAMM = ModBlocks.BLOCKS.register("funkelholz_stamm",
            () -> new RotatedPillarBlock(holzProps(MapColor.COLOR_MAGENTA)));
    public static final DeferredBlock<Block> FUNKELHOLZ_BRETTER = ModBlocks.BLOCKS.register("funkelholz_bretter",
            () -> new Block(holzProps(MapColor.COLOR_PINK)));
    public static final DeferredBlock<LeavesBlock> FUNKELLAUB = ModBlocks.BLOCKS.register("funkellaub",
            () -> new LeavesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.2f).randomTicks()
                    .sound(SoundType.CHERRY_LEAVES).noOcclusion().lightLevel(s -> 4)
                    .isValidSpawn((s, l, p, t) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                    .ignitedByLava().pushReaction(PushReaction.DESTROY).isRedstoneConductor((s, l, p) -> false)));
    public static final DeferredBlock<DekoLichtBlock> SCHWEBELATERNE = ModBlocks.BLOCKS.register("schwebelaterne",
            () -> new DekoLichtBlock(false, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.0f)
                    .sound(SoundType.LANTERN).lightLevel(s -> 15).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<DekoLichtBlock> FEENLICHT = ModBlocks.BLOCKS.register("feenlicht",
            () -> new DekoLichtBlock(true, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.1f)
                    .sound(SoundType.AMETHYST_CLUSTER).lightLevel(s -> 15).noCollission().noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    /** Alle neuen Blöcke mit eigenem Item, in Handbuch-Reihenfolge. */
    public static final List<DeferredBlock<? extends Block>> MIT_ITEM = new ArrayList<>();

    static {
        MIT_ITEM.addAll(PFLANZEN.values());
        MIT_ITEM.addAll(List.of(LICHTRANKE, KRAEUTERBUENDEL, DACHZIEGEL, DACHZIEGEL_TREPPE, DACHZIEGEL_STUFE, FACHWERK,
                FACHWERK_KREUZ, BUNTES_KOPFSTEINPFLASTER, STERNGLAS, FUNKELHOLZ_STAMM, FUNKELHOLZ_BRETTER, FUNKELLAUB,
                SCHWEBELATERNE, FEENLICHT));
        for (DeferredBlock<? extends Block> b : MIT_ITEM) {
            ModItems.ITEMS.register(b.getId().getPath(), () -> new BlockItem(b.get(), new Item.Properties()));
        }
    }

    // ------------------------------------------------------------------ Items

    public static final DeferredItem<Item> STERNENSTAUB = ModItems.ITEMS.register("sternenstaub",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> STERNEN_CHIP = ModItems.ITEMS.register("sternen_chip",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> STERNENBROT = ModItems.ITEMS.register("sternenbrot",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8f)
                    .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 600), 1.0f).build())));
    public static final DeferredItem<Item> GLIMMHAAR = ModItems.ITEMS.register("glimmhaar", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FLAUSCH = ModItems.ITEMS.register("flausch", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FALTERSTAUB = ModItems.ITEMS.register("falterstaub", () -> new Item(new Item.Properties()));

    // ------------------------------------------------------------------ Spawn-Eier

    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> GLEEB_SPAWN_EGG = ModItems.ITEMS.register(
            "gleeb_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.GLEEB, 0x7A3AC8, 0xFFD060, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> GLIMMHASE_SPAWN_EGG = ModItems.ITEMS.register(
            "glimmhase_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.GLIMMHASE, 0xE8E0F8, 0x9AF0FF, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> PUSTELING_SPAWN_EGG = ModItems.ITEMS.register(
            "pusteling_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.PUSTELING, 0xFFE0F4, 0xC8A0FF, new Item.Properties()));
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> SCHIMMERFALTER_SPAWN_EGG = ModItems.ITEMS.register(
            "schimmerfalter_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.SCHIMMERFALTER, 0xB07AFF, 0x7AFFE8, new Item.Properties()));

    // ------------------------------------------------------------------ Effekte

    public static final DeferredHolder<MobEffect, dev.riftverse.dorf.VerhuehnertEffect> VERHUEHNERT = EFFECTS.register("verhuehnert",
            dev.riftverse.dorf.VerhuehnertEffect::new);

    private ModDorf() {
    }

    /** Lädt die Klasse, damit sich alles rechtzeitig einträgt. */
    public static void init() {
    }

    private static DeferredBlock<LeuchtpflanzeBlock> pflanze(String name, Art art, int farbe) {
        Supplier<LeuchtpflanzeBlock> s = () -> new LeuchtpflanzeBlock(art, farbe, pflanzenProps(MapColor.PLANT)
                .lightLevel(st -> LeuchtLogik.licht(st, art.nacht, art.tag)));
        DeferredBlock<LeuchtpflanzeBlock> b = ModBlocks.BLOCKS.register(name, s);
        PFLANZEN.put(name, b);
        return b;
    }

    private static BlockBehaviour.Properties pflanzenProps(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).noCollission().instabreak().sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties steinProps(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(1.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties holzProps(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(2.0f, 3.0f).sound(SoundType.CHERRY_WOOD).ignitedByLava();
    }
}
