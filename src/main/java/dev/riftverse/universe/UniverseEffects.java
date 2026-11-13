package dev.riftverse.universe;

import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Physik, Gefahren und Modifikatoren der Universen.
 * <ul>
 *   <li>Gravitation über das Gravitations-Attribut; Fallschaden skaliert mit g (die T1-Typen haben 1 g).</li>
 *   <li>Toxische Welt: Luft vergiftet ohne Helm, Säureregen frisst Rüstung an.</li>
 *   <li>Kristallwelt: Lärm (Sprinten) lässt Fluxkristall-Cluster in der Nähe zerspringen.</li>
 *   <li>Modifikatoren: Riesen-/Mini-Mobs, keine Tiere, Gewitter, Hüpfen, Leuchten.</li>
 * </ul>
 * Universen sind beim Betreten friedlich: Es gibt hier keinen Boss, nur die Umwelt des Typs.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class UniverseEffects {
    private static final ResourceLocation GRAVITY = Riftverse.id("universe_gravity");
    private static final ResourceLocation SAFE_FALL = Riftverse.id("universe_safe_fall");
    private static final ResourceLocation FALL_DAMAGE = Riftverse.id("universe_fall_damage");
    private static final ResourceLocation SCALE = Riftverse.id("universe_scale");
    private static final ResourceLocation STEP = Riftverse.id("universe_step");
    /** Markiert Doppelgänger der Spiegelwelt. */
    public static final String DOPPELGAENGER = "riftverse:doppelgaenger";

    private static final String WARN_KEY = "riftverse:warned";

    // ------------------------------------------------------------------ Spieler

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;

        UniverseProfile p = Multiverse.profileAt(level, player.blockPosition());
        if (player.tickCount % 10 == 0) applyGravity(player, p);
        if (p == null || player.isCreative() || player.isSpectator()) return;

        switch (p.type) {
            case TOXISCH -> {
                if (player.tickCount % 20 == 0) toxicAir(player);
                if (player.tickCount % 40 == 0) acidRain(level, player);
            }
            case KRISTALL -> {
                if (player.tickCount % 10 == 0 && player.isSprinting()) resonance(level, player);
            }
            case SPIEGEL -> {
                if (player.tickCount % 600 == 0) doppelgaenger(level, player);
            }
            case HOCHGRAVITATION -> {
                if (player.tickCount % 200 == 0) warn(player, "gravity",
                        Component.translatable("message.riftverse.hochgravitation").withStyle(ChatFormatting.GOLD));
            }
            default -> { }
        }
        if (p.has(UniverseModifier.GEWITTER) && player.tickCount % 200 == 0) lightning(level, player);
        if (p.has(UniverseModifier.HUEPFEN) && player.tickCount % 20 == 0) hop(level, player);
    }

    private static void toxicAir(Player player) {
        if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return;
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true));
        warn(player, "toxic_air", Component.translatable("message.riftverse.toxic_air").withStyle(ChatFormatting.DARK_GREEN));
    }

    private static void acidRain(ServerLevel level, Player player) {
        if (!level.isRainingAt(player.blockPosition().above())) return;
        boolean armor = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) continue;
            stack.hurtAndBreak(1, player, slot);
            armor = true;
        }
        if (!armor) player.hurt(ModDamageTypes.source(level, ModDamageTypes.SAEURE), 1.0f);
        level.playSound(null, player.blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.WEATHER, 0.25f, 1.8f);
        warn(player, "acid_rain", Component.translatable("message.riftverse.acid_rain").withStyle(ChatFormatting.YELLOW));
    }

    /** Lärm lässt Kristalle explodieren: Sprinten bringt Cluster im Umkreis von 4 Blöcken zum Schwingen. */
    private static void resonance(ServerLevel level, Player player) {
        BlockPos center = player.blockPosition();
        List<BlockPos> clusters = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -2, -4), center.offset(4, 3, 4))) {
            if (level.getBlockState(pos).is(ModBlocks.FLUXKRISTALL_CLUSTER.get())) clusters.add(pos.immutable());
        }
        if (clusters.isEmpty()) return;
        warn(player, "resonance", Component.translatable("message.riftverse.resonance").withStyle(ChatFormatting.GREEN));
        for (BlockPos pos : clusters) {
            if (level.random.nextFloat() >= 0.08f) continue;
            level.destroyBlock(pos, false);
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.5f, 0.6f);
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.5f, Level.ExplosionInteraction.NONE);
        }
    }

    private static void lightning(ServerLevel level, Player player) {
        if (level.random.nextFloat() > 0.3f) return;
        double angle = level.random.nextDouble() * Math.PI * 2;
        double dist = 8 + level.random.nextDouble() * 24;
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(Vec3.atBottomCenterOf(new BlockPos(x, y, z)));
        level.addFreshEntity(bolt);
    }

    /**
     * Spiegelwelt: Ab und zu taucht ein Doppelgänger auf, der die Ausrüstung des Spielers kopiert. Die Kopien
     * fallen nicht herunter. Höchstens zwei Doppelgänger pro Spieler gleichzeitig.
     */
    private static void doppelgaenger(ServerLevel level, Player player) {
        if (level.random.nextFloat() > 0.2f) return;
        int count = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48),
                m -> m.getPersistentData().getBoolean(DOPPELGAENGER)).size();
        if (count >= 2) return;
        double angle = level.random.nextDouble() * Math.PI * 2;
        double dist = 12 + level.random.nextDouble() * 8;
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (Math.abs(y - player.getY()) > 12) return;
        net.minecraft.world.entity.monster.Husk twin = EntityType.HUSK.create(level);
        if (twin == null) return;
        twin.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360f, 0f);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ANIMAL_ARMOR) continue;
            ItemStack copy = player.getItemBySlot(slot).copy();
            twin.setItemSlot(slot, copy);
            twin.setDropChance(slot, 0.0f);
        }
        twin.setCustomName(Component.translatable("entity.riftverse.doppelgaenger", player.getName()));
        twin.getPersistentData().putBoolean(DOPPELGAENGER, true);
        twin.setPersistenceRequired();
        level.addFreshEntity(twin);
        warn(player, "doppelgaenger", Component.translatable("message.riftverse.doppelgaenger").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static void hop(ServerLevel level, Player player) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24))) {
            if (mob.onGround() && level.random.nextFloat() < 0.15f) mob.getJumpControl().jump();
        }
    }

    /** Hinweis höchstens einmal pro Minute und Art. */
    private static void warn(Player player, String what, Component message) {
        String key = WARN_KEY + "/" + what;
        long now = player.level().getGameTime();
        if (now - player.getPersistentData().getLong(key) < 1200) return;
        player.getPersistentData().putLong(key, now);
        player.displayClientMessage(message, true);
    }

    // ------------------------------------------------------------------ Kreaturen

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living) || living instanceof Player) return;
        UniverseProfile p = Multiverse.profileAt(event.getLevel(), living.blockPosition());
        if (p == null) return;
        applyGravity(living, p);
        double scale = p.has(UniverseModifier.RIESEN_MOBS) ? 1.6 : p.has(UniverseModifier.MINI_MOBS) ? 0.5 : 1.0;
        set(living, Attributes.SCALE, SCALE, scale - 1.0);
        if (p.has(UniverseModifier.LEUCHTEN)) {
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        }
    }

    /** Modifikator „Keine Tiere“: Tiere spawnen nicht von selbst. Mitgebrachte oder gezüchtete bleiben. */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Animal)) return;
        MobSpawnType t = event.getSpawnType();
        if (t != MobSpawnType.NATURAL && t != MobSpawnType.CHUNK_GENERATION && t != MobSpawnType.SPAWNER
                && t != MobSpawnType.STRUCTURE) return;
        UniverseProfile p = Multiverse.profileAt(event.getLevel().getLevel(), BlockPos.containing(event.getX(), event.getY(), event.getZ()));
        if (p != null && p.has(UniverseModifier.KEINE_TIERE)) event.setSpawnCancelled(true);
    }

    // ------------------------------------------------------------------ Gravitation

    public static void applyGravity(LivingEntity e, @Nullable UniverseProfile p) {
        double g = p == null ? 1.0 : p.gravity;
        boolean heavy = p != null && p.type == UniverseType.HOCHGRAVITATION;
        set(e, Attributes.GRAVITY, GRAVITY, g - 1.0);
        set(e, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL, 1.0 / g - 1.0);
        // Hochgravitation: Fallschaden 3×, schon kleine Stürze sind tödlich
        set(e, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_DAMAGE, heavy ? 2.0 : g - 1.0);
        // Springen geht dort kaum noch; damit man nicht in jeder Mulde festsitzt, steigt man volle Blöcke hoch
        set(e, Attributes.STEP_HEIGHT, STEP, heavy ? 1.0 / 0.6 - 1.0 : 0.0);
    }

    private static void set(LivingEntity e, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance inst = e.getAttribute(attribute);
        if (inst == null) return;
        if (Math.abs(amount) < 1.0e-4) {
            if (inst.getModifier(id) != null) inst.removeModifier(id);
            return;
        }
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) < 1.0e-4) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private UniverseEffects() {}
}
