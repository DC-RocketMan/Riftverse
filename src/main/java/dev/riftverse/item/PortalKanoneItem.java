package dev.riftverse.item;

import dev.riftverse.fluid.Zerfall;
import dev.riftverse.portal.Heimanker;
import dev.riftverse.portal.RiftPortalEntity;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModDataComponents.KanonenTank;
import dev.riftverse.registry.ModItems;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Portal-Kanone. Mk I: rein zufällige Sprünge in T1-Universen, keine Auswahl, 250 mB Roh-Portalfluid pro Schuss.
 * <ul>
 *   <li>Rechtsklick: Rift in ein zufälliges Universum öffnen.</li>
 *   <li>Schleichen + Rechtsklick oder Rückkehr-Taste (Standard R): Rift zum Heimanker öffnen.</li>
 * </ul>
 * Die Kanone fasst 1.000 mB. Ist sie leer, lädt sie automatisch eine volle Fluid-Kartusche aus dem Inventar nach.
 */
public class PortalKanoneItem extends Item {
    public static final int TANK = 1_000;

    private final int tier;

    public PortalKanoneItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    /** Fluid pro Schuss: 250 mB (Mk I) bis 25 mB (Mk IV). */
    public int shotCost() {
        return switch (tier) {
            case 1 -> 250;
            case 2 -> 100;
            case 3 -> 50;
            default -> 25;
        };
    }

    public static KanonenTank tank(ItemStack gun) {
        return gun.getOrDefault(ModDataComponents.KANONEN_TANK.get(), KanonenTank.LEER);
    }

    // ------------------------------------------------------------------ Benutzung

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(gun);
        boolean ok = player.isShiftKeyDown() ? returnHome(sp, gun) : jump(sp, gun);
        return ok ? InteractionResultHolder.success(gun) : InteractionResultHolder.fail(gun);
    }

    /** Rift in ein zufälliges Universum der erreichbaren Typen. */
    public boolean jump(ServerPlayer player, ItemStack gun) {
        if (player.getCooldowns().isOnCooldown(this)) return false;
        ServerLevel level = player.serverLevel();
        UniverseId here = Multiverse.at(level, player.blockPosition());
        if (!fire(player, gun)) return false;
        if (here == null) Heimanker.set(player, level.dimension(), player.position());

        List<UniverseType> types = UniverseType.reachableWith(tier);
        UniverseId target = Multiverse.random(level.random, types);
        for (int i = 0; i < 4 && target.equals(here); i++) target = Multiverse.random(level.random, types);

        Vec3 spot = aimedSpot(level, player);
        RiftPortalEntity portal = RiftPortalEntity.create(level, spot, player.getYRot() + 180f, RiftPortalEntity.STYLE_RANDOM, 600);
        portal.setUniverseTarget(target.type().dimension(), target);
        portal.setSpawnReturn(false);
        level.addFreshEntity(portal);
        effects(level, player, spot);
        player.displayClientMessage(Component.translatable("message.riftverse.rift_open").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    /** Rift zum Heimanker, direkt vor dem Spieler (Rückkehr-Taste oder Schleichen + Rechtsklick). */
    public boolean returnHome(ServerPlayer player, ItemStack gun) {
        if (player.getCooldowns().isOnCooldown(this)) return false;
        ServerLevel level = player.serverLevel();
        if (!Multiverse.isMultiverse(level)) {
            player.displayClientMessage(Component.translatable("message.riftverse.already_home").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        if (!fire(player, gun)) return false;
        Heimanker home = Heimanker.of(player);
        Vec3 spot = groundBelow(level, player.position().add(Vec3.directionFromRotation(0f, player.getYRot()).scale(2.5)));
        RiftPortalEntity portal = RiftPortalEntity.create(level, spot, player.getYRot() + 180f, RiftPortalEntity.STYLE_HOME, 400);
        portal.setExactTarget(home.dimension(), home.pos());
        portal.setSpawnReturn(false);
        level.addFreshEntity(portal);
        effects(level, player, spot);
        player.displayClientMessage(Component.translatable("message.riftverse.rift_home").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /**
     * Zieht einen Schuss Fluid ab und lädt bei Bedarf eine volle Kartusche nach.
     * @return false, wenn kein (unzerfallenes) Roh-Portalfluid da ist
     */
    public boolean fire(ServerPlayer player, ItemStack gun) {
        if (player.getAbilities().instabuild) {
            player.getCooldowns().addCooldown(this, 20);
            return true;
        }
        long now = Zerfall.now(player.level());
        KanonenTank tank = tank(gun);
        if (tank.mb() > 0 && Zerfall.zerfallen(tank.zerfall(), now)) {
            tank = KanonenTank.LEER;
            player.displayClientMessage(Component.translatable("message.riftverse.kanone_zerfallen").withStyle(ChatFormatting.GRAY), false);
        }
        if (tank.mb() < shotCost()) {
            ItemStack cartridge = takeCartridge(player, now);
            if (cartridge == null) {
                gun.set(ModDataComponents.KANONEN_TANK.get(), tank);
                player.displayClientMessage(Component.translatable("message.riftverse.kein_fluid").withStyle(ChatFormatting.RED), true);
                player.level().playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1f, 1.2f);
                return false;
            }
            long z = FluidKartuscheItem.zerfall(cartridge);
            tank = new KanonenTank(Math.min(TANK, tank.mb() + FluidKartuscheItem.INHALT), tank.mb() > 0 ? Zerfall.frueher(tank.zerfall(), z) : z);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.8f, 1.6f);
        }
        int left = tank.mb() - shotCost();
        gun.set(ModDataComponents.KANONEN_TANK.get(), left > 0 ? new KanonenTank(left, tank.zerfall()) : KanonenTank.LEER);
        player.getCooldowns().addCooldown(this, 40);
        return true;
    }

    /** Nimmt eine volle, unzerfallene Kartusche aus dem Inventar und legt dafür eine leere zurück. */
    @Nullable
    private static ItemStack takeCartridge(ServerPlayer player, long now) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.is(ModItems.FLUID_KARTUSCHE_VOLL.get())) continue;
            if (FluidKartuscheItem.zerfallen(stack, now)) {
                inv.setItem(i, new ItemStack(ModItems.FLUID_KARTUSCHE.get(), stack.getCount()));
                continue;
            }
            ItemStack taken = stack.split(1);
            ItemStack empty = new ItemStack(ModItems.FLUID_KARTUSCHE.get());
            if (!inv.add(empty)) player.drop(empty, false);
            return taken;
        }
        return null;
    }

    /** Erste Portal-Kanone im Inventar (für die Rückkehr-Taste). */
    @Nullable
    public static ItemStack find(Player player) {
        if (player.getMainHandItem().getItem() instanceof PortalKanoneItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof PortalKanoneItem) return player.getOffhandItem();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof PortalKanoneItem) return inv.getItem(i);
        }
        return null;
    }

    // ------------------------------------------------------------------ Hilfen

    private static final DustParticleOptions TRAIL = new DustParticleOptions(new Vector3f(0.22f, 1.0f, 0.42f), 1.0f);

    private static void effects(ServerLevel level, Player player, Vec3 spot) {
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.9f);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.6f);
        Vec3 eye = player.getEyePosition();
        Vec3 dir = spot.add(0, 1.2, 0).subtract(eye);
        for (int i = 0; i < 20; i++) {
            Vec3 p = eye.add(dir.scale(i / 20.0));
            level.sendParticles(TRAIL, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    /** Portal an der anvisierten Stelle (max. 32 Blöcke), auf dem Boden stehend. */
    private static Vec3 aimedSpot(Level level, Player player) {
        HitResult hit = player.pick(32, 1.0f, false);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 spot;
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 h = hit.getLocation();
            spot = h.subtract(look.scale(0.7));
            if (hit instanceof BlockHitResult bhr && bhr.getDirection() == Direction.UP) {
                spot = h.subtract(look.x * 0.7, 0, look.z * 0.7);
            }
        } else {
            spot = eye.add(look.scale(4));
        }
        return groundBelow(level, spot);
    }

    private static Vec3 groundBelow(Level level, Vec3 spot) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(spot).mutable();
        for (int i = 0; i < 4 && level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty(); i++) {
            pos.move(0, -1, 0);
        }
        return new Vec3(spot.x, pos.getY(), spot.z);
    }

    // ------------------------------------------------------------------ Zerfall im Tank, Anzeige

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || entity.tickCount % 20 != 0) return;
        KanonenTank tank = tank(stack);
        if (tank.mb() > 0 && Zerfall.zerfallen(tank.zerfall(), Zerfall.now(level))) {
            stack.set(ModDataComponents.KANONEN_TANK.get(), KanonenTank.LEER);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return tank(stack).mb() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * tank(stack).mb() / TANK);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x39FF6A;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        KanonenTank tank = tank(stack);
        tooltip.add(Component.translatable("tooltip.riftverse.kanone_tank", tank.mb(), TANK, shotCost()).withStyle(ChatFormatting.GRAY));
        Level level = context.level();
        if (tank.mb() > 0 && tank.zerfall() > 0 && level != null) {
            tooltip.add(Component.translatable("tooltip.riftverse.zerfaellt_in", Zerfall.rest(tank.zerfall(), level.getGameTime()))
                    .withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("tooltip.riftverse.kanone_mk" + tier).withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.riftverse.kanone_bedienung").withStyle(ChatFormatting.DARK_GRAY));
    }
}
