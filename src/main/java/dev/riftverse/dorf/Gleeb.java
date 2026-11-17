package dev.riftverse.dorf;

import dev.riftverse.Riftverse;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Gleebs (M10-10 bis M10-14): kleine, freundliche Magier in Bridge's Inn. Sie leben in ihren Dörfern, handeln
 * (je nach Beruf, siehe {@link Handel}), gehen nachts heim – manche spazieren aber durch die Leuchtpflanzen und
 * zaubern dort offen. Einige fliegen auf Besen über ihr Dorf. Wer einen Gleeb schlägt, wird verhühnert
 * ({@link Huhn}); Gleebs selbst nehmen keinen Schaden.
 */
public class Gleeb extends PathfinderMob implements Merchant {
    private static final EntityDataAccessor<Integer> DATA_BERUF = SynchedEntityData.defineId(Gleeb.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BESEN = SynchedEntityData.defineId(Gleeb.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ZAUBER = SynchedEntityData.defineId(Gleeb.class, EntityDataSerializers.INT);

    private static final String[] SILBEN_A = {"Pim", "Wuz", "Fiz", "Lu", "Bro", "Glim", "Tup", "Mo", "Kri", "Nib", "Zot", "Ple", "Quib", "Sni"};
    private static final String[] SILBEN_B = {"bo", "zel", "zle", "mi", "dle", "pel", "sy", "ko", "wick", "bert", "li", "nus", "ka", "ri"};

    @Nullable
    private BlockPos zuhause, mitte;
    private int radius = 32;
    private boolean nachtschwaermer;
    @Nullable
    private MerchantOffers angebote;
    @Nullable
    private Player kunde;
    private long auffuellTag = -1;
    private int xp;

    public Gleeb(EntityType<? extends Gleeb> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation nav) nav.setCanOpenDoors(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BERUF, 0);
        builder.define(DATA_BESEN, false);
        builder.define(DATA_ZAUBER, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HandelGoal());
        goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        goalSelector.addGoal(3, new HeimGoal());
        goalSelector.addGoal(4, new ZauberGoal());
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                return !besen() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5) {
            @Override
            public boolean canUse() {
                return !besen() && (!level().isNight() || nachtschwaermer) && super.canUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // ------------------------------------------------------------------ Einrichten

    /** Beim Erzeugen eines Dorfs: Beruf, Zuhause, Dorfmitte und -radius. */
    public void einrichten(Beruf beruf, BlockPos zuhause, BlockPos mitte, int radius, RandomSource r) {
        entityData.set(DATA_BERUF, beruf.ordinal());
        this.zuhause = zuhause.immutable();
        this.mitte = mitte.immutable();
        this.radius = radius;
        this.nachtschwaermer = r.nextInt(10) < 3;
        setCustomName(Component.literal(SILBEN_A[r.nextInt(SILBEN_A.length)] + SILBEN_B[r.nextInt(SILBEN_B.length)]));
        restrictTo(mitte, radius);
    }

    @Override
    @SuppressWarnings("deprecation")
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type,
                                        @Nullable SpawnGroupData data) {
        if (zuhause == null) {
            Beruf b = Beruf.von(1 + random.nextInt(Beruf.values().length - 1));
            einrichten(random.nextInt(3) == 0 ? Beruf.BUERGER : b, blockPosition(), blockPosition(), 32, random);
        }
        return super.finalizeSpawn(level, difficulty, type, data);
    }

    public Beruf beruf() {
        return Beruf.von(entityData.get(DATA_BERUF));
    }

    public boolean besen() {
        return entityData.get(DATA_BESEN);
    }

    public void besenSetzen(boolean b) {
        entityData.set(DATA_BESEN, b);
        setNoGravity(b);
    }

    /** Zaubert gerade (Arme hoch, Funken)? */
    public boolean zaubert() {
        return entityData.get(DATA_ZAUBER) > 0;
    }

    // ------------------------------------------------------------------ Leben

    @Override
    public void aiStep() {
        super.aiStep();
        int z = entityData.get(DATA_ZAUBER);
        if (z > 0 && !level().isClientSide) entityData.set(DATA_ZAUBER, z - 1);
        if (level().isClientSide) {
            if (zaubert() && random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.ENCHANT, getRandomX(0.8), getY() + 1.4 + random.nextDouble() * 0.4,
                        getRandomZ(0.8), (random.nextDouble() - 0.5) * 0.6, 0.3, (random.nextDouble() - 0.5) * 0.6);
            }
            if (besen() && random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 0, -0.02, 0);
            }
            return;
        }
        if (besen()) fliegen();
        if (kunde != null && (!kunde.isAlive() || !(kunde.containerMenu instanceof MerchantMenu) || distanceToSqr(kunde) > 256)) {
            setTradingPlayer(null);
        }
        if (tickCount % 100 == 0) {
            long tag = level().getDayTime() / 24000L;
            if (tag != auffuellTag) {
                auffuellTag = tag;
                if (angebote != null) angebote.forEach(MerchantOffer::resetUses);
            }
            if (zuhause == null) {
                zuhause = blockPosition();
                mitte = blockPosition();
            }
            if (!hasRestriction() && mitte != null) restrictTo(mitte, radius);
        }
    }

    /** Besenflug: große, langsame Kreise über der Dorfmitte. */
    private void fliegen() {
        fallDistance = 0;
        BlockPos m = mitte != null ? mitte : blockPosition();
        double t = (tickCount + getId() * 37L) * 0.008;
        double r = Math.max(14, radius * 0.6);
        Vec3 ziel = new Vec3(m.getX() + 0.5 + Math.cos(t) * r, m.getY() + 15 + Math.sin(t * 3.1) * 3, m.getZ() + 0.5 + Math.sin(t) * r);
        Vec3 d = ziel.subtract(position());
        Vec3 v = d.lengthSqr() > 4 ? d.normalize().scale(0.28) : d.scale(0.1);
        setDeltaMovement(v);
        if (v.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(v.z, v.x) * (180F / Math.PI)) - 90F;
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (!level().isClientSide && source.getEntity() instanceof ServerPlayer p) Huhn.verhuehnern(p, this);
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || besen()) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (Huhn.ist(player)) {
            playSound(SoundEvents.WITCH_CELEBRATE, 1.0f, 1.4f);
            player.displayClientMessage(Component.translatable("dorf.riftverse.huhn_kein_handel", getName()), true);
            return InteractionResult.CONSUME;
        }
        if (!beruf().handelt()) {
            playSound(SoundEvents.ALLAY_AMBIENT_WITH_ITEM, 1.0f, 1.3f);
            player.displayClientMessage(Component.translatable("dorf.riftverse.gruss." + (1 + random.nextInt(6)), getName()), true);
            getLookControl().setLookAt(player);
            return InteractionResult.CONSUME;
        }
        if (kunde != null && kunde != player) {
            player.displayClientMessage(Component.translatable("dorf.riftverse.beschaeftigt", getName()), true);
            return InteractionResult.CONSUME;
        }
        setTradingPlayer(player);
        openTradingScreen(player, getDisplayName().copy().append(" · ")
                .append(Component.translatable("beruf.riftverse." + beruf().id)), 1);
        return InteractionResult.CONSUME;
    }

    // ------------------------------------------------------------------ Händler

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.kunde = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return kunde;
    }

    @Override
    public MerchantOffers getOffers() {
        if (angebote == null) angebote = Handel.angebote(beruf());
        return angebote;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.angebote = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        ambientSoundTime = -getAmbientSoundInterval();
        xp += offer.getXp();
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.2, getZ(), 6, 0.3, 0.3, 0.3, 0);
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        if (!level().isClientSide && ambientSoundTime > -getAmbientSoundInterval() + 20) {
            ambientSoundTime = -getAmbientSoundInterval();
            playSound(stack.isEmpty() ? SoundEvents.ALLAY_HURT : SoundEvents.ALLAY_AMBIENT_WITH_ITEM, 0.8f, 1.3f);
        }
    }

    @Override
    public int getVillagerXp() {
        return xp;
    }

    @Override
    public void overrideXp(int xp) {
        this.xp = xp;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide;
    }

    // ------------------------------------------------------------------ Klänge

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.3f + (random.nextFloat() - 0.5f) * 0.2f;
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Beruf", beruf().id);
        tag.putBoolean("Besen", besen());
        tag.putBoolean("Nachtschwaermer", nachtschwaermer);
        tag.putInt("Radius", radius);
        tag.putLong("AuffuellTag", auffuellTag);
        tag.putInt("Xp", xp);
        if (zuhause != null) tag.put("Zuhause", NbtUtils.writeBlockPos(zuhause));
        if (mitte != null) tag.put("Mitte", NbtUtils.writeBlockPos(mitte));
        if (angebote != null) {
            MerchantOffers.CODEC.encodeStart(registryAccess().createSerializationContext(NbtOps.INSTANCE), angebote)
                    .result().ifPresent(t -> tag.put("Angebote", t));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_BERUF, Beruf.von(tag.getString("Beruf")).ordinal());
        besenSetzen(tag.getBoolean("Besen"));
        nachtschwaermer = tag.getBoolean("Nachtschwaermer");
        if (tag.contains("Radius")) radius = tag.getInt("Radius");
        auffuellTag = tag.getLong("AuffuellTag");
        xp = tag.getInt("Xp");
        NbtUtils.readBlockPos(tag, "Zuhause").ifPresent(p -> zuhause = p);
        NbtUtils.readBlockPos(tag, "Mitte").ifPresent(p -> mitte = p);
        if (tag.contains("Angebote")) {
            MerchantOffers.CODEC.parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.get("Angebote"))
                    .resultOrPartial(e -> Riftverse.LOGGER.warn("Gleeb-Angebote: {}", e)).ifPresent(o -> angebote = o);
        }
        if (mitte != null) restrictTo(mitte, radius);
    }

    // ------------------------------------------------------------------ Ziele

    /** Beim Handeln stehen bleiben und den Kunden ansehen. */
    private final class HandelGoal extends Goal {
        HandelGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return kunde != null && !besen();
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (kunde != null) getLookControl().setLookAt(kunde, 30f, 30f);
        }
    }

    /** Nachts nach Hause (außer die Nachtschwärmer). */
    private final class HeimGoal extends Goal {
        HeimGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !besen() && !nachtschwaermer && zuhause != null && level().isNight()
                    && distanceToSqr(Vec3.atBottomCenterOf(zuhause)) > 6;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && !getNavigation().isDone();
        }

        @Override
        public void start() {
            getNavigation().moveTo(zuhause.getX() + 0.5, zuhause.getY(), zuhause.getZ() + 0.5, 0.6);
        }
    }

    /** Nachts zu einer Leuchtpflanze gehen und dort zaubern: Funken, Klang, alles ringsum leuchtet auf. */
    private final class ZauberGoal extends Goal {
        @Nullable
        private BlockPos ziel;
        private int dauer;

        ZauberGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (besen() || !nachtschwaermer || !level().isNight() || random.nextInt(240) != 0) return false;
            ziel = sucheLeuchtpflanze();
            return ziel != null;
        }

        @Override
        public boolean canContinueToUse() {
            return ziel != null && dauer < 400 && level().isNight();
        }

        @Override
        public void start() {
            dauer = 0;
            getNavigation().moveTo(ziel.getX() + 0.5, ziel.getY(), ziel.getZ() + 0.5, 0.5);
        }

        @Override
        public void stop() {
            ziel = null;
        }

        @Override
        public void tick() {
            dauer++;
            if (ziel == null) return;
            getLookControl().setLookAt(Vec3.atCenterOf(ziel));
            if (distanceToSqr(Vec3.atBottomCenterOf(ziel)) < 6.25 || getNavigation().isDone()) {
                getNavigation().stop();
                if (entityData.get(DATA_ZAUBER) == 0 && level() instanceof ServerLevel sl) {
                    entityData.set(DATA_ZAUBER, 60);
                    LeuchtLogik.anregen(sl, ziel, 3);
                    sl.playSound(null, ziel, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 0.8f, 1.2f);
                    sl.sendParticles(ParticleTypes.WITCH, ziel.getX() + 0.5, ziel.getY() + 0.8, ziel.getZ() + 0.5, 20, 1.2, 0.4, 1.2, 0.02);
                    sl.sendParticles(ParticleTypes.END_ROD, ziel.getX() + 0.5, ziel.getY() + 1, ziel.getZ() + 0.5, 12, 1.0, 0.6, 1.0, 0.03);
                    dauer = 340;
                }
            }
        }

        @Nullable
        private BlockPos sucheLeuchtpflanze() {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int i = 0; i < 40; i++) {
                p.set(getBlockX() + random.nextInt(25) - 12, getBlockY() + random.nextInt(5) - 2, getBlockZ() + random.nextInt(25) - 12);
                BlockState s = level().getBlockState(p);
                if (s.hasProperty(LeuchtLogik.ANGEREGT) && s.getBlock() instanceof LeuchtpflanzeBlock) return p.immutable();
            }
            return null;
        }
    }
}
