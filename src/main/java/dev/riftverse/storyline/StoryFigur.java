package dev.riftverse.storyline;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Grundfigur für Storylines: Rechtsklick spricht sie an (Dialog über ihrem Kopf), sie ist unverwundbar, verschwindet
 * nie von selbst und läuft auf Wunsch in einem Radius um ihren Platz herum. Name und Aussehen kommen aus der Story-Datei.
 * Die magischen Bewohner aus Meilenstein 10 bauen auf ihr auf.
 */
public class StoryFigur extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_AUSSEHEN =
            SynchedEntityData.defineId(StoryFigur.class, EntityDataSerializers.STRING);
    /** So lange (Ticks) bleibt die Figur nach dem Ansprechen stehen und schaut den Spieler an. */
    public static final int REDEN_TICKS = 200;

    private String storyline = "";
    private String figur = "";
    private boolean laufen;
    private int radius = 6;
    private int redetTicks;
    @Nullable
    private UUID partner;

    public StoryFigur(EntityType<? extends StoryFigur> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AUSSEHEN, "steve");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ZuhoerenGoal());
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                return laufen && redetTicks <= 0 && super.canUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                return laufen && redetTicks <= 0 && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Richtet die Figur nach der Story-Datei ein. */
    public void einrichten(Storyline s, Storyline.Figur f, BlockPos pos) {
        storyline = s.id().toString();
        figur = f.id();
        laufen = f.laufen();
        radius = Math.max(1, f.radius());
        setCustomName(Component.literal(f.name()));
        setCustomNameVisible(true);
        entityData.set(DATA_AUSSEHEN, f.aussehen());
        moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        restrictTo(pos, radius);
    }

    public String storyline() {
        return storyline;
    }

    public String figur() {
        return figur;
    }

    public String aussehen() {
        return entityData.get(DATA_AUSSEHEN);
    }

    /** Die Figur bleibt stehen und schaut den Spieler an. */
    public void sprichtMit(Player player) {
        redetTicks = REDEN_TICKS;
        partner = player.getUUID();
        getNavigation().stop();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (redetTicks > 0) redetTicks--;
        // Vom Welt-Stand vergessen (verschwinden lassen, Storyline zurückgesetzt): Figur räumt sich selbst weg
        if (!level().isClientSide && tickCount % 100 == 0 && !StoryManager.gueltig(this)) discard();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) StoryManager.angesprochen(sp, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Storyline", storyline);
        tag.putString("Figur", figur);
        tag.putString("Aussehen", aussehen());
        tag.putBoolean("Laufen", laufen);
        tag.putInt("Radius", radius);
        tag.putLong("Heim", getRestrictCenter().asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        storyline = tag.getString("Storyline");
        figur = tag.getString("Figur");
        if (tag.contains("Aussehen")) entityData.set(DATA_AUSSEHEN, tag.getString("Aussehen"));
        laufen = tag.getBoolean("Laufen");
        radius = Math.max(1, tag.getInt("Radius"));
        if (tag.contains("Heim")) restrictTo(BlockPos.of(tag.getLong("Heim")), radius);
        setInvulnerable(true);
    }

    /** Steht still und schaut den Gesprächspartner an, solange das Gespräch läuft. */
    private final class ZuhoerenGoal extends Goal {
        ZuhoerenGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return redetTicks > 0;
        }

        @Override
        public void tick() {
            getNavigation().stop();
            if (partner != null && level().getPlayerByUUID(partner) instanceof Player p) {
                getLookControl().setLookAt(p, 30f, 30f);
            }
        }
    }
}
