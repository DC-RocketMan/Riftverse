package dev.riftverse.boss;

import dev.riftverse.portal.RiftPortalEntity;
import dev.riftverse.registry.ModItems;
import dev.riftverse.story.BossPlot;
import dev.riftverse.story.MultiverseData;
import dev.riftverse.story.StoryEngine;
import dev.riftverse.story.Twist;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ein Rift-Boss. Alles, was er sagt, kommt in Echtzeit aus der {@link StoryEngine}:
 * Begrüßung, Reaktionen auf Treffer, Phasenwechsel, Plot-Twist, Betteln um Gnade,
 * Spott nach einem Sieg und der Epilog. Am Ende gibt es eine Chronik als Buch.
 */
public class RiftBossEntity extends Monster {
    public static final String MINION_TAG = "riftverse_minion";
    private static final EntityDataAccessor<Integer> DATA_SKIN =
            SynchedEntityData.defineId(RiftBossEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("Rift-Boss"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    @Nullable private BossPlot plot;
    @Nullable private UniverseId universe;
    @Nullable private BlockPos arena;
    private double baseDamage = 7;

    private int phase;
    private boolean greeted, fightStarted, begged, twistDone, resolved;
    private int begTicks;
    private int castTicks;
    @Nullable private BossAbility casting;
    private final int[] cooldowns = new int[BossAbility.values().length];
    private boolean slamPending;
    private int slamTicks;
    private long fightStartTick;
    private long lastTalk;

    private final List<String> log = new ArrayList<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();

    public RiftBossEntity(EntityType<? extends RiftBossEntity> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.8);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
            @Override
            public boolean canUse() {
                return !RiftBossEntity.this.isBusy() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !RiftBossEntity.this.isBusy() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN, 0);
    }

    public int getSkin() {
        return entityData.get(DATA_SKIN);
    }

    private boolean isBusy() {
        return begTicks > 0 || castTicks > 0;
    }

    @Nullable
    private UniverseProfile profile() {
        return universe != null ? UniverseProfile.get(universe) : null;
    }

    // ================================================================== Einrichtung

    public void setup(BossPlot plot, @Nullable UniverseId universe, double health, double damage, @Nullable BlockPos arena) {
        this.plot = plot;
        this.universe = universe;
        this.arena = arena;
        this.baseDamage = damage;
        entityData.set(DATA_SKIN, plot.skin);
        base(Attributes.MAX_HEALTH, health);
        setHealth((float) health);
        base(Attributes.ATTACK_DAMAGE, damage);
        base(Attributes.SCALE, 1.35);
        setCustomName(Component.literal(plot.fullName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        setCustomNameVisible(true);
        if (arena != null) restrictTo(arena, 10);
        addEffect(new MobEffectInstance(MobEffects.GLOWING, MobEffectInstance.INFINITE_DURATION, 0, false, false));

        ItemStack held = switch (plot.personality) {
            case BUREAUCRAT -> new ItemStack(Items.WRITABLE_BOOK);
            case SALESMAN -> new ItemStack(Items.EMERALD);
            case NIHILIST -> new ItemStack(Items.ECHO_SHARD);
            case MANIAC -> new ItemStack(Items.BLAZE_ROD);
            case COWARD -> new ItemStack(Items.SPYGLASS);
            case AVENGER -> new ItemStack(Items.RECOVERY_COMPASS);
            default -> new ItemStack(Items.END_ROD);
        };
        setItemSlot(EquipmentSlot.MAINHAND, held);
        setDropChance(EquipmentSlot.MAINHAND, 0f);
    }

    private void base(Holder<Attribute> attribute, double value) {
        AttributeInstance inst = getAttribute(attribute);
        if (inst != null) inst.setBaseValue(value);
    }

    /** Boss aus dem Spawn-Ei: Handlung wird spontan erfunden. */
    private void initSpontaneous(ServerLevel level) {
        UniverseId id = Multiverse.at(level, blockPosition());
        UniverseProfile u = id != null ? UniverseProfile.get(id) : null;
        setup(StoryEngine.createPlot(random, u), id, 150 + (u != null ? u.danger * 20 : 0), 7, null);
        removeEffect(MobEffects.GLOWING);
    }

    // ================================================================== Sprechen

    private StoryEngine.Ctx ctx(@Nullable Player p) {
        String name = p != null ? p.getName().getString() : "Reisender";
        float ph = p != null ? p.getHealth() / p.getMaxHealth() : 1f;
        int d = p != null ? deaths.getOrDefault(p.getUUID(), 0) : 0;
        String held = p != null && !p.getMainHandItem().isEmpty() ? p.getMainHandItem().getHoverName().getString() : "";
        int secs = fightStarted ? (int) ((level().getGameTime() - fightStartTick) / 20) : 0;
        return new StoryEngine.Ctx(name, ph, getHealth() / getMaxHealth(), profile(), d, held, secs);
    }

    public void speak(String text) {
        if (!(level() instanceof ServerLevel sl) || text.isEmpty()) return;
        Component msg = Component.literal("<" + (plot != null ? plot.name : "???") + "> ")
                .withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE));
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) < 96 * 96) p.sendSystemMessage(msg);
        }
        log.add(text);
        if (log.size() > 14) log.remove(0);
        lastTalk = sl.getGameTime();
    }

    private void say(StoryEngine.Event event, @Nullable Player p) {
        if (plot != null) speak(StoryEngine.line(plot, event, ctx(p), random));
    }

    // ================================================================== KI-Tick

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        if (plot == null) initSpontaneous(sl);
        bossEvent.setProgress(getHealth() / getMaxHealth());

        Player nearest = sl.getNearestPlayer(this, 28);
        if (!greeted) {
            if (tickCount % 10 == 0) beam(sl);
            if (nearest != null && !nearest.isSpectator()) {
                greeted = true;
                removeEffect(MobEffects.GLOWING);
                sl.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5f, 1.4f);
                say(StoryEngine.Event.GREET, nearest);
            }
        }

        LivingEntity target = getTarget();
        if (!fightStarted && greeted && target instanceof Player tp && distanceToSqr(tp) < 100) startFight(tp);

        if (begTicks > 0) {
            tickBegging(sl, nearest);
            return;
        }
        tickAbilities(sl, target);
        tickSlam(sl);

        if (fightStarted && target instanceof Player tp && distanceToSqr(tp) > 22 * 22
                && sl.getGameTime() - lastTalk > 400) {
            say(StoryEngine.Event.IDLE, tp);
        }
    }

    private void startFight(@Nullable Player p) {
        if (fightStarted) return;
        fightStarted = true;
        fightStartTick = level().getGameTime();
        clearRestriction();
        say(StoryEngine.Event.FIGHT, p);
    }

    /** Lichtsäule, damit man den Boss aus der Ferne findet. */
    private void beam(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) > 200 * 200) continue;
            for (int i = 0; i < 14; i++) {
                sl.sendParticles(p, ParticleTypes.END_ROD, true, getX(), getY() + 3 + i * 2.5, getZ(), 2, 0.1, 0.6, 0.1, 0.01);
            }
        }
    }

    // ================================================================== Gnade

    private void startBegging(@Nullable Player p) {
        begged = true;
        begTicks = 200;
        castTicks = 0;
        casting = null;
        getNavigation().stop();
        setTarget(null);
        say(StoryEngine.Event.BEG, p);
        if (level() instanceof ServerLevel sl) {
            for (ServerPlayer sp : sl.players()) {
                if (sp.distanceToSqr(this) < 40 * 40) {
                    sp.displayClientMessage(Component.literal("Rechtsklick auf " + (plot != null ? plot.name : "den Boss")
                            + " = verschonen · Angreifen = beenden").withStyle(ChatFormatting.GOLD), true);
                }
            }
        }
    }

    private void tickBegging(ServerLevel sl, @Nullable Player nearest) {
        begTicks--;
        getNavigation().stop();
        setTarget(null);
        if (tickCount % 8 == 0) {
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + getBbHeight() + 0.4, getZ(), 3, 0.3, 0.2, 0.3, 0.01);
        }
        if (begTicks == 0) {
            say(StoryEngine.Event.BEG_REFUSED, nearest);
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (begTicks > 0 && hand == InteractionHand.MAIN_HAND) {
            if (!level().isClientSide && player instanceof ServerPlayer sp) spare(sp);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private void spare(ServerPlayer player) {
        if (resolved || !(level() instanceof ServerLevel sl)) return;
        begTicks = 0;
        say(StoryEngine.Event.SPARED, player);
        RiftPortalEntity portal = RiftPortalEntity.create(sl, position(), getYRot(), RiftPortalEntity.STYLE_ESCAPE, 50);
        portal.setTravelEnabled(false);
        sl.addFreshEntity(portal);
        resolve(StoryEngine.Outcome.SPARED, player);
        dismissMinions(sl);
        discard();
    }

    // ================================================================== Schaden & Phasen

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (begTicks > 170) return false; // kurze Schonfrist, damit man die Bitte lesen kann
        boolean result = super.hurt(source, amount);
        if (!result || level().isClientSide || plot == null || isDeadOrDying()) return result;

        Player attacker = source.getEntity() instanceof Player p ? p : null;
        if (attacker != null && !fightStarted) {
            startFight(attacker);
        } else if (attacker != null && begTicks == 0 && random.nextFloat() < 0.3f
                && level().getGameTime() - lastTalk > 120) {
            say(StoryEngine.Event.HURT, attacker);
        }

        float pct = getHealth() / getMaxHealth();
        if (phase == 0 && pct < 0.66f) {
            phase = 1;
            say(StoryEngine.Event.PHASE2, attacker);
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        }
        if (phase == 1 && pct < 0.33f) {
            phase = 2;
            say(StoryEngine.Event.PHASE3, attacker);
            revealTwist(attacker);
        }
        if (!begged && plot.willBeg && pct < 0.2f) startBegging(attacker);
        return result;
    }

    private void revealTwist(@Nullable Player p) {
        if (twistDone || plot == null || plot.twist == Twist.NONE || !(level() instanceof ServerLevel sl)) return;
        twistDone = true;
        speak(StoryEngine.twistReveal(plot, ctx(p)));
        switch (plot.twist) {
            case CLONE -> spawnClones(sl);
            case PHYSICS -> {
                for (Player pl : nearbyPlayers(16)) pl.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0));
                ring(sl, ParticleTypes.REVERSE_PORTAL, 8);
            }
            case ALTERNATE_SELF -> entityData.set(DATA_SKIN, 3);
            case PUPPET -> addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
            case VICTIM -> addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
            default -> { }
        }
    }

    public void onKilledPlayer(ServerPlayer player) {
        deaths.merge(player.getUUID(), 1, Integer::sum);
        say(StoryEngine.Event.KILL, player);
    }

    @Override
    public void die(DamageSource source) {
        boolean wasBegging = begTicks > 0;
        ServerPlayer killer = source.getEntity() instanceof ServerPlayer sp ? sp : null;
        if (!level().isClientSide && plot != null && !resolved) {
            say(wasBegging ? StoryEngine.Event.DEFEAT_BEGGING : StoryEngine.Event.DEFEAT, killer);
        }
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            resolve(wasBegging ? StoryEngine.Outcome.EXECUTED : StoryEngine.Outcome.DEFEATED, killer);
            dismissMinions(sl);
        }
    }

    /** Belohnung, Epilog, Chronik-Buch und Speicherstand. */
    private void resolve(StoryEngine.Outcome outcome, @Nullable ServerPlayer player) {
        if (resolved || plot == null || !(level() instanceof ServerLevel sl)) return;
        resolved = true;
        UniverseProfile u = profile();
        String playerName = player != null ? player.getName().getString() : "Reisender";
        String epithet = u != null ? StoryEngine.epithet(outcome, u, random) : "Rift-Wanderer";
        List<String> epilogue = StoryEngine.epilogue(plot, outcome, u, playerName, epithet);

        int cells = outcome == StoryEngine.Outcome.SPARED ? 6 : 3 + random.nextInt(4);
        spawnAtLocation(new ItemStack(ModItems.RIFT_FLUID_CELL.get(), cells));
        spawnAtLocation(new ItemStack(ModItems.QUANTUM_INGOT.get(), 2 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(ModItems.UNIVERSE_SHARD.get()));
        ItemStack book = StoryEngine.chronicle(plot, outcome, u, log, epilogue);
        if (player == null || !player.getInventory().add(book)) spawnAtLocation(book);

        String header = switch (outcome) {
            case SPARED -> "VERSCHONT";
            case EXECUTED -> "GERICHTET";
            default -> "UNIVERSUM BEFREIT";
        };
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) > 128 * 128) continue;
            p.sendSystemMessage(Component.literal("✦ " + header + " ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            for (String line : epilogue) {
                p.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            }
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(header).withStyle(ChatFormatting.GOLD)));
        }
        sl.playSound(null, blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);

        if (universe != null) {
            MultiverseData data = MultiverseData.get(sl.getServer());
            MultiverseData.Record rec = data.record(universe);
            rec.state = outcome == StoryEngine.Outcome.SPARED ? MultiverseData.STATE_SPARED : MultiverseData.STATE_DEFEATED;
            rec.epithet = epithet;
            rec.bossName = plot.fullName();
            data.markLiberated();
        }
        bossEvent.removeAllPlayers();
    }

    // ================================================================== Fähigkeiten

    private void tickAbilities(ServerLevel sl, @Nullable LivingEntity target) {
        for (int i = 0; i < cooldowns.length; i++) if (cooldowns[i] > 0) cooldowns[i]--;

        if (castTicks > 0) {
            castTicks--;
            getNavigation().stop();
            sl.sendParticles(ParticleTypes.WITCH, getX(), getY() + getBbHeight() + 0.3, getZ(), 2, 0.4, 0.2, 0.4, 0.01);
            if (castTicks == 0 && casting != null) {
                execute(sl, casting, target);
                casting = null;
            }
            return;
        }
        if (!fightStarted || plot == null || target == null || !target.isAlive()) return;
        if (random.nextInt(25) != 0) return;

        BossAbility a = plot.abilities.get(random.nextInt(plot.abilities.size()));
        if (cooldowns[a.ordinal()] > 0 || distanceTo(target) > a.maxRange) return;
        cooldowns[a.ordinal()] = Math.max(60, a.cooldown - phase * 40);
        casting = a;
        castTicks = 16;
        if (random.nextFloat() < 0.6f) {
            speak(StoryEngine.abilityLine(plot, a, ctx(target instanceof Player p ? p : null), random));
        }
        playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.5f, 0.8f);
    }

    private void execute(ServerLevel sl, BossAbility ability, @Nullable LivingEntity target) {
        float dmg = (float) baseDamage;
        UniverseProfile u = profile();
        switch (ability) {
            case GRAVITY_PULSE -> {
                for (Player p : nearbyPlayers(12)) {
                    Vec3 dir = position().subtract(p.position()).normalize();
                    p.setDeltaMovement(dir.scale(1.1).add(0, 0.45, 0));
                    p.hurtMarked = true;
                    p.hurt(damageSources().mobAttack(this), dmg * 0.5f);
                }
                ring(sl, ParticleTypes.REVERSE_PORTAL, 6);
                playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.5f, 0.6f);
            }
            case PLASMA_BEAM -> {
                if (target == null || !hasLineOfSight(target)) return;
                Vec3 from = getEyePosition();
                Vec3 delta = target.getEyePosition().subtract(from);
                int steps = Math.max(1, (int) (delta.length() * 3));
                for (int i = 0; i <= steps; i++) {
                    Vec3 pt = from.add(delta.scale(i / (double) steps));
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                }
                target.hurt(damageSources().indirectMagic(this, this), dmg * 1.1f);
                if (u != null && u.temperature == UniverseProfile.Temperature.HOT) target.setRemainingFireTicks(60);
                playSound(SoundEvents.GUARDIAN_ATTACK, 1.5f, 1.4f);
            }
            case SUMMON_MINIONS -> summonMinions(sl, target, 2 + phase);
            case BLINK -> {
                if (target == null) return;
                Vec3 look = target.getLookAngle().multiply(1, 0, 1);
                if (look.lengthSqr() < 1.0e-4) look = new Vec3(1, 0, 0);
                Vec3 behind = target.position().subtract(look.normalize().scale(2.0));
                sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 40, 0.5, 1, 0.5, 0.5);
                teleportTo(behind.x, target.getY(), behind.z);
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.8f);
                doHurtTarget(target);
            }
            case TIME_WARP -> {
                for (Player p : nearbyPlayers(14)) {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                    p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1));
                }
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
                ring(sl, ParticleTypes.ENCHANT, 8);
                playSound(SoundEvents.BEACON_DEACTIVATE, 1.5f, 0.5f);
            }
            case GROUND_SLAM -> {
                Vec3 m = getDeltaMovement();
                setDeltaMovement(m.x, 1.0, m.z);
                hasImpulse = true;
                slamPending = true;
                slamTicks = 0;
                playSound(SoundEvents.RAVAGER_ROAR, 1.2f, 1.2f);
            }
        }
    }

    /** Aufprall nach dem Sprung – bei niedriger Gravitation dauert der Flug länger. */
    private void tickSlam(ServerLevel sl) {
        if (!slamPending) return;
        slamTicks++;
        if (slamTicks > 5 && onGround()) {
            slamPending = false;
            UniverseProfile u = profile();
            double g = u != null ? u.gravity : 1.0;
            float dmg = (float) (baseDamage * (0.6 + 0.5 * g));
            for (Player p : nearbyPlayers(7)) {
                p.hurt(damageSources().mobAttack(this), dmg);
                p.knockback(1.4, getX() - p.getX(), getZ() - p.getZ());
                p.hurtMarked = true;
            }
            ring(sl, ParticleTypes.EXPLOSION, 4);
            ring(sl, ParticleTypes.CLOUD, 6);
            playSound(SoundEvents.ANVIL_LAND, 1.5f, 0.5f);
        } else if (slamTicks > 160) {
            slamPending = false;
        }
    }

    private void summonMinions(ServerLevel sl, @Nullable LivingEntity target, int count) {
        if (plot == null) return;
        int existing = sl.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(32), m -> m.getTags().contains(MINION_TAG)).size();
        count = Math.min(count, 6 - existing);
        UniverseProfile u = profile();
        EntityType<? extends Mob> type = u == null ? EntityType.ZOMBIE : switch (u.temperature) {
            case HOT -> EntityType.HUSK;
            case COLD -> EntityType.STRAY;
            default -> EntityType.ZOMBIE;
        };
        for (int i = 0; i < count; i++) {
            Mob m = type.create(sl);
            if (m == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            m.moveTo(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, random.nextFloat() * 360f, 0f);
            m.setCustomName(Component.literal(plot.minions).withStyle(ChatFormatting.GRAY));
            m.addTag(MINION_TAG);
            m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            m.setDropChance(EquipmentSlot.HEAD, 0f);
            if (m instanceof AbstractSkeleton) m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            if (target != null) m.setTarget(target);
            sl.addFreshEntity(m);
            sl.sendParticles(ParticleTypes.PORTAL, m.getX(), m.getY() + 1, m.getZ(), 20, 0.3, 0.6, 0.3, 0.3);
        }
        playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5f, 1f);
    }

    private void spawnClones(ServerLevel sl) {
        if (plot == null) return;
        for (int i = 0; i < 2; i++) {
            Mob clone = EntityType.ZOMBIE.create(sl);
            if (clone == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            clone.moveTo(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, getYRot(), 0f);
            clone.setCustomName(Component.literal(plot.name + " (Klon)").withStyle(ChatFormatting.LIGHT_PURPLE));
            clone.setCustomNameVisible(true);
            clone.addTag(MINION_TAG);
            clone.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            clone.setDropChance(EquipmentSlot.HEAD, 0f);
            clone.setItemSlot(EquipmentSlot.MAINHAND, getMainHandItem().copy());
            clone.setDropChance(EquipmentSlot.MAINHAND, 0f);
            clone.setTarget(getTarget());
            sl.addFreshEntity(clone);
        }
        sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 60, 1, 1, 1, 0.5);
    }

    private void dismissMinions(ServerLevel sl) {
        for (Mob m : sl.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(48), m -> m.getTags().contains(MINION_TAG))) {
            sl.sendParticles(ParticleTypes.PORTAL, m.getX(), m.getY() + 1, m.getZ(), 20, 0.3, 0.6, 0.3, 0.3);
            m.discard();
        }
    }

    private List<Player> nearbyPlayers(double radius) {
        return level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius),
                p -> p.isAlive() && !p.isSpectator());
    }

    private void ring(ServerLevel sl, ParticleOptions particle, double radius) {
        for (int i = 0; i < 24; i++) {
            double a = i / 24.0 * Math.PI * 2;
            sl.sendParticles(particle, getX() + Math.cos(a) * radius, getY() + 0.2, getZ() + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    // ================================================================== Bossleiste & Persistenz

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        if (name != null) bossEvent.setName(name);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (plot != null) tag.put("Plot", plot.save());
        if (universe != null) tag.put("Universe", universe.save());
        if (arena != null) tag.putLong("Arena", arena.asLong());
        tag.putDouble("BaseDamage", baseDamage);
        tag.putInt("Phase", phase);
        tag.putBoolean("Greeted", greeted);
        tag.putBoolean("Fight", fightStarted);
        tag.putBoolean("Begged", begged);
        tag.putBoolean("TwistDone", twistDone);
        ListTag logTag = new ListTag();
        for (String s : log) logTag.add(StringTag.valueOf(s));
        tag.put("Log", logTag);
        CompoundTag d = new CompoundTag();
        deaths.forEach((k, v) -> d.putInt(k.toString(), v));
        tag.put("Deaths", d);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Plot")) {
            plot = BossPlot.load(tag.getCompound("Plot"));
            entityData.set(DATA_SKIN, plot.skin);
        }
        universe = UniverseId.load(tag.getCompound("Universe"));
        if (tag.contains("Arena")) arena = BlockPos.of(tag.getLong("Arena"));
        baseDamage = tag.contains("BaseDamage") ? tag.getDouble("BaseDamage") : 7;
        phase = tag.getInt("Phase");
        greeted = tag.getBoolean("Greeted");
        fightStarted = tag.getBoolean("Fight");
        begged = tag.getBoolean("Begged");
        twistDone = tag.getBoolean("TwistDone");
        if (twistDone && plot != null && plot.twist == Twist.ALTERNATE_SELF) entityData.set(DATA_SKIN, 3);
        log.clear();
        ListTag logTag = tag.getList("Log", Tag.TAG_STRING);
        for (int i = 0; i < logTag.size(); i++) log.add(logTag.getString(i));
        deaths.clear();
        CompoundTag d = tag.getCompound("Deaths");
        for (String k : d.getAllKeys()) {
            try {
                deaths.put(UUID.fromString(k), d.getInt(k));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (getCustomName() != null) bossEvent.setName(getCustomName());
    }
}
