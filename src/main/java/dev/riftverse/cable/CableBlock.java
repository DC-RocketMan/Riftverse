package dev.riftverse.cable;

import dev.riftverse.Config;
import dev.riftverse.energy.EnergyNet;
import dev.riftverse.energy.EnergyNetwork;
import dev.riftverse.energy.RiftEnergyConnector;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Riftverse-Leitung im IndustrialCraft-Stil: ein dünner Kern in der Blockmitte, Arme nur zu
 * verbundenen Seiten. Verbindet sich nur mit Riftverse-Blöcken.
 */
public class CableBlock extends Block {
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;

    private final VoltageTier tier;
    private final double lossPerBlock;
    private final boolean insulated;
    private final float shockDamage;
    private final VoxelShape[] shapes;

    /**
     * @param thicknessPx Dicke in Pixeln (von 16), z. B. 4 blank, 6 isoliert
     */
    public CableBlock(Properties properties, VoltageTier tier, double lossPerBlock, int thicknessPx, boolean insulated) {
        this(properties, tier, lossPerBlock, thicknessPx, insulated, insulated ? 0.0f : 2.0f);
    }

    /**
     * @param shockDamage Schaden des Schlags bei Berührung unter Strom (0 = sicher). Die Gravitit-Leitung gibt blank
     *                    einen starken Schlag und mit nur einer Isolierschicht noch einen normalen.
     */
    public CableBlock(Properties properties, VoltageTier tier, double lossPerBlock, int thicknessPx, boolean insulated,
                      float shockDamage) {
        super(properties);
        this.tier = tier;
        this.lossPerBlock = lossPerBlock;
        this.insulated = insulated;
        this.shockDamage = shockDamage;
        this.shapes = makeShapes(thicknessPx / 2.0);
        BlockState state = stateDefinition.any();
        for (BooleanProperty p : PROPERTY_BY_DIRECTION.values()) state = state.setValue(p, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (BooleanProperty p : PROPERTY_BY_DIRECTION.values()) builder.add(p);
    }

    /** Höchste Spannungsstufe, die diese Leitung verträgt. */
    public VoltageTier tier() {
        return tier;
    }

    /** Verlust in FE pro Block und Paket. */
    public double lossPerBlock() {
        return lossPerBlock;
    }

    public boolean insulated() {
        return insulated;
    }

    /** Schaden des Schlags; 0 = sicher anzufassen. */
    public float shockDamage() {
        return shockDamage;
    }

    // ------------------------------------------------------------------ Verbindungen

    public static int color(BlockState state) {
        return state.hasProperty(InsulatedCableBlock.COLOR) ? state.getValue(InsulatedCableBlock.COLOR) : 0;
    }

    /** Unterschiedlich gefärbte Kabel verbinden sich nicht; ungefärbte verbinden sich mit allen. */
    public static boolean compatible(BlockState a, BlockState b) {
        int ca = color(a), cb = color(b);
        return ca == 0 || cb == 0 || ca == cb;
    }

    protected boolean connectsTo(BlockState self, BlockGetter level, BlockPos pos, Direction dir) {
        BlockState other = level.getBlockState(pos.relative(dir));
        if (other.getBlock() instanceof CableBlock) return compatible(self, other);
        if (other.getBlock() instanceof RiftEnergyConnector c) return c.canConnectEnergy(other, dir.getOpposite());
        return false;
    }

    protected BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(d), connectsTo(state, level, pos, d));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(state, level, pos, direction));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock()) || color(oldState) != color(state)) EnergyNet.markDirty(level);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(state.getBlock())) EnergyNet.markDirty(level);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ------------------------------------------------------------------ Form

    private static VoxelShape[] makeShapes(double r) {
        double lo = 8 - r, hi = 8 + r;
        VoxelShape core = Block.box(lo, lo, lo, hi, hi, hi);
        VoxelShape[] arms = new VoxelShape[6];
        arms[Direction.DOWN.ordinal()] = Block.box(lo, 0, lo, hi, lo, hi);
        arms[Direction.UP.ordinal()] = Block.box(lo, hi, lo, hi, 16, hi);
        arms[Direction.NORTH.ordinal()] = Block.box(lo, lo, 0, hi, hi, lo);
        arms[Direction.SOUTH.ordinal()] = Block.box(lo, lo, hi, hi, hi, 16);
        arms[Direction.WEST.ordinal()] = Block.box(0, lo, lo, lo, hi, hi);
        arms[Direction.EAST.ordinal()] = Block.box(hi, lo, lo, 16, hi, hi);
        VoxelShape[] result = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape s = core;
            for (Direction d : Direction.values()) {
                if ((mask & (1 << d.ordinal())) != 0) s = Shapes.or(s, arms[d.ordinal()]);
            }
            result[mask] = s;
        }
        return result;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(d))) mask |= 1 << d.ordinal();
        }
        return shapes[mask];
    }

    // ------------------------------------------------------------------ Strom

    /** Blanke (oder zu dünn isolierte) Leitung unter Strom: Schlag bei Berührung. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (shockDamage <= 0.0f || level.isClientSide || !(entity instanceof LivingEntity living)) return;
        if (level.getGameTime() % 10 != 0 || !Config.CABLE_SHOCK.get()) return;
        if (!EnergyNet.isLive(level, pos)) return;
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ModDamageTypes.STROMSCHLAG));
        living.hurt(source, shockDamage);
    }

    /** Rechtsklick mit leerer Hand: Kennzahlen des Teilnetzes in der Aktionsleiste. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) {
            EnergyNetwork.Info info = EnergyNet.info(sl, pos);
            if (info == null) {
                player.displayClientMessage(Component.translatable("message.riftverse.net.empty"), true);
            } else {
                boolean live = sl.getGameTime() - info.lastActive() <= 2;
                player.displayClientMessage(Component.translatable("message.riftverse.net.info",
                        info.sources(), info.sinks(), info.cables(), live ? info.lastTransferred() : 0, tier.displayName()), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Paket zu groß für die Leitung: sie schmilzt. */
    public static void meltdown(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof CableBlock)) return;
        level.removeBlock(pos, false);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 8, 0.2, 0.2, 0.2, 0.01);
        level.sendParticles(ParticleTypes.LAVA, x, y, z, 3, 0.1, 0.1, 0.1, 0.0);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.8f);
    }
}
