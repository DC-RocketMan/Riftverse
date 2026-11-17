package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.boss.RiftBossEntity;
import dev.riftverse.entity.SchrottGolem;
import dev.riftverse.portal.RiftPortalEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Riftverse.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<RiftPortalEntity>> RIFT_PORTAL =
            ENTITIES.register("rift_portal", () -> EntityType.Builder.<RiftPortalEntity>of(RiftPortalEntity::new, MobCategory.MISC)
                    .sized(1.4f, 2.4f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .fireImmune()
                    .build("rift_portal"));

    public static final DeferredHolder<EntityType<?>, EntityType<RiftBossEntity>> RIFT_BOSS =
            ENTITIES.register("rift_boss", () -> EntityType.Builder.<RiftBossEntity>of(RiftBossEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 2.0f)
                    .clientTrackingRange(12)
                    .build("rift_boss"));

    /** Gefahr der Schrottwelt: Schrott-Golems in den Halden. */
    public static final DeferredHolder<EntityType<?>, EntityType<SchrottGolem>> SCHROTT_GOLEM =
            ENTITIES.register("schrott_golem", () -> EntityType.Builder.<SchrottGolem>of(SchrottGolem::new, MobCategory.MONSTER)
                    .sized(1.4f, 2.7f)
                    .clientTrackingRange(10)
                    .build("schrott_golem"));

    // --- Meilenstein 6: Gadgets ---
    public static final DeferredHolder<EntityType<?>, EntityType<dev.riftverse.gadget.HoloKoederEntity>> HOLO_KOEDER =
            ENTITIES.register("holo_koeder", () -> EntityType.Builder.<dev.riftverse.gadget.HoloKoederEntity>of(
                    dev.riftverse.gadget.HoloKoederEntity::new, MobCategory.MISC).sized(0.6f, 1.8f).noSave().fireImmune()
                    .clientTrackingRange(8).build("holo_koeder"));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.riftverse.gadget.AntiGravGranateEntity>> ANTI_GRAV_GRANATE =
            ENTITIES.register("anti_grav_granate", () -> EntityType.Builder.<dev.riftverse.gadget.AntiGravGranateEntity>of(
                    dev.riftverse.gadget.AntiGravGranateEntity::new, MobCategory.MISC).sized(0.25f, 0.25f)
                    .clientTrackingRange(4).updateInterval(10).build("anti_grav_granate"));

    // --- Neue Universen, Meilenstein 9: Story-Gerüst ---
    public static final DeferredHolder<EntityType<?>, EntityType<dev.riftverse.storyline.StoryFigur>> STORY_FIGUR =
            ENTITIES.register("story_figur", () -> EntityType.Builder.<dev.riftverse.storyline.StoryFigur>of(
                    dev.riftverse.storyline.StoryFigur::new, MobCategory.MISC).sized(0.6f, 1.8f)
                    .clientTrackingRange(10).build("story_figur"));

    // --- Neue Universen, Meilenstein 10: Bridge's Inn ---
    public static final DeferredHolder<EntityType<?>, EntityType<dev.riftverse.dorf.Gleeb>> GLEEB =
            ENTITIES.register("gleeb", () -> EntityType.Builder.<dev.riftverse.dorf.Gleeb>of(
                    dev.riftverse.dorf.Gleeb::new, MobCategory.MISC).sized(0.5f, 1.45f).eyeHeight(1.25f)
                    .clientTrackingRange(10).build("gleeb"));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.riftverse.dorf.Glimmhase>> GLIMMHASE =
            ENTITIES.register("glimmhase", () -> EntityType.Builder.<dev.riftverse.dorf.Glimmhase>of(
                    dev.riftverse.dorf.Glimmhase::new, MobCategory.CREATURE).sized(0.5f, 0.55f)
                    .clientTrackingRange(8).build("glimmhase"));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.riftverse.dorf.Pusteling>> PUSTELING =
            ENTITIES.register("pusteling", () -> EntityType.Builder.<dev.riftverse.dorf.Pusteling>of(
                    dev.riftverse.dorf.Pusteling::new, MobCategory.CREATURE).sized(0.6f, 0.6f)
                    .clientTrackingRange(8).build("pusteling"));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.riftverse.dorf.Schimmerfalter>> SCHIMMERFALTER =
            ENTITIES.register("schimmerfalter", () -> EntityType.Builder.<dev.riftverse.dorf.Schimmerfalter>of(
                    dev.riftverse.dorf.Schimmerfalter::new, MobCategory.CREATURE).sized(0.5f, 0.4f)
                    .clientTrackingRange(8).build("schimmerfalter"));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.riftverse.dorf.Irrlicht>> IRRLICHT =
            ENTITIES.register("irrlicht", () -> EntityType.Builder.<dev.riftverse.dorf.Irrlicht>of(
                    dev.riftverse.dorf.Irrlicht::new, MobCategory.MISC).sized(0.4f, 0.4f).noSave().fireImmune()
                    .clientTrackingRange(8).updateInterval(2).build("irrlicht"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GLEEB.get(), dev.riftverse.dorf.Gleeb.createAttributes().build());
        event.put(GLIMMHASE.get(), dev.riftverse.dorf.Glimmhase.createAttributes().build());
        event.put(PUSTELING.get(), dev.riftverse.dorf.Pusteling.createAttributes().build());
        event.put(SCHIMMERFALTER.get(), dev.riftverse.dorf.Schimmerfalter.createAttributes().build());
        event.put(STORY_FIGUR.get(), dev.riftverse.storyline.StoryFigur.createAttributes().build());
        event.put(HOLO_KOEDER.get(), dev.riftverse.gadget.HoloKoederEntity.createAttributes().build());
        event.put(RIFT_BOSS.get(), RiftBossEntity.createAttributes().build());
        event.put(SCHROTT_GOLEM.get(), SchrottGolem.createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(SCHROTT_GOLEM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SchrottGolem::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GLIMMHASE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PUSTELING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(SCHIMMERFALTER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private ModEntities() {}
}
