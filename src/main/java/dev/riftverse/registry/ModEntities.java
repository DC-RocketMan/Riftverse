package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.boss.RiftBossEntity;
import dev.riftverse.portal.RiftPortalEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
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

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(RIFT_BOSS.get(), RiftBossEntity.createAttributes().build());
    }

    private ModEntities() {}
}
