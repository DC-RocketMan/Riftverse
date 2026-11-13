package dev.riftverse.item;

import dev.riftverse.fluid.Zerfall;
import dev.riftverse.portal.Heimanker;
import dev.riftverse.portal.RiftPortalEntity;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModDataComponents.KanonenTank;
import dev.riftverse.registry.ModDataComponents.KanonenZiele;
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
 * Portal-Kanone. Mk I: rein zufällige Sprünge, 250 mB pro Schuss (Roh-Portalfluid: T1-Universen, stabilisiertes
 * Portalfluid: auch T2). Mk II: stabilisiertes Portalfluid oder Konzentrat, 100 mB pro Schuss, Koordinatenkarten;
 * mit Konzentrat springt sie zufällig auch in T3-Welten. Mk III: nur Konzentrat, 50 mB pro Schuss, merkt sich bis zu
 * 8 Ziele (Taste V wechselt: Zufall → Kurzstrecke → Ziel 1 … 8) und öffnet Kurzstrecken-Portale in derselben Welt;
 * mit Weltenformer-Essenz springt sie zufällig auch in die Leere Welt. Mk IV: nur Weltenformer-Essenz, 25 mB pro
 * Schuss, alles von Mk III und dazu die gezüchteten Universen des Weltenformers (über deren Koordinatenkarte).
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

    /** Reichweite der Kurzstrecken-Portale (Mk III). */
    public static final int KURZSTRECKE_WEITE = 96;

    public static KanonenTank tank(ItemStack gun) {
        return gun.getOrDefault(ModDataComponents.KANONEN_TANK.get(), KanonenTank.LEER);
    }

    public static KanonenZiele ziele(ItemStack gun) {
        return gun.getOrDefault(ModDataComponents.KANONEN_ZIELE.get(), KanonenZiele.LEER);
    }

    /**
     * Welche Fluid-Stufen (1 roh, 2 stabil, 3 Konzentrat, 4 Weltenformer-Essenz) diese Kanone verträgt. Jede Kanone
     * nimmt ihre eigene Stufe und die nächsthöhere, mit der sie dann zufällig auch eine Stufe weiter springt;
     * die Mk IV läuft nur mit Essenz.
     */
    public boolean accepts(int stufe) {
        return switch (tier) {
            case 1 -> stufe >= 1 && stufe <= 2;
            case 2 -> stufe >= 2 && stufe <= 3;
            case 3 -> stufe >= 3 && stufe <= 4;
            default -> stufe == 4;
        };
    }

    // ------------------------------------------------------------------ Benutzung

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(gun);
        boolean ok = player.isShiftKeyDown() ? returnHome(sp, gun) : jump(sp, gun);
        return ok ? InteractionResultHolder.success(gun) : InteractionResultHolder.fail(gun);
    }

    /**
     * Rift in ein Universum. Mk I springt rein zufällig: mit Roh-Portalfluid in die drei T1-Typen, mit
     * stabilisiertem Portalfluid auch in die T2-Typen. Ab Mk II führt eine Koordinatenkarte in der anderen Hand
     * gezielt in ihr Universum.
     */
    public boolean jump(ServerPlayer player, ItemStack gun) {
        if (player.getCooldowns().isOnCooldown(this)) return false;
        ServerLevel level = player.serverLevel();
        UniverseId here = Multiverse.at(level, player.blockPosition());

        UniverseId mapped = tier >= 2 ? mapTarget(player, gun) : null;
        boolean fromCard = mapped != null;
        KanonenZiele ziele = ziele(gun);
        if (mapped == null && tier >= 3) {
            if (ziele.auswahl() == KanonenZiele.KURZSTRECKE) return kurzstrecke(player, gun);
            mapped = ziele.gewaehlt();
        }
        if (mapped != null && mapped.equals(here)) {
            player.displayClientMessage(Component.translatable("message.riftverse.schon_dort").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        KanonenTank shot = shoot(player, gun);
        if (shot == null) return false;
        if (here == null) Heimanker.set(player, level.dimension(), player.position());

        UniverseId target;
        if (mapped != null) {
            target = mapped;
        } else {
            List<UniverseType> types = UniverseType.randomWith(Math.max(tier, shot.stufe()));
            target = Multiverse.random(level.random, types);
            for (int i = 0; i < 4 && target.equals(here); i++) target = Multiverse.random(level.random, types);
        }

        Vec3 spot = aimedSpot(level, player);
        RiftPortalEntity portal = RiftPortalEntity.create(level, spot, player.getYRot() + 180f, RiftPortalEntity.STYLE_RANDOM, 600);
        portal.setUniverseTarget(target.type().dimension(), target);
        portal.setSpawnReturn(false);
        level.addFreshEntity(portal);
        effects(level, player, spot);
        if (fromCard && tier >= 3) gun.set(ModDataComponents.KANONEN_ZIELE.get(), ziele.merken(mapped));
        if (mapped != null) {
            player.displayClientMessage(Component.translatable("message.riftverse.rift_ziel", "Universum " + target.code(),
                    target.type().displayName()).withStyle(ChatFormatting.GREEN), true);
        } else {
            player.displayClientMessage(Component.translatable("message.riftverse.rift_open").withStyle(ChatFormatting.GREEN), true);
        }
        return true;
    }

    /**
     * Mk III, Auswahl „Kurzstrecke“: Rift direkt vor dem Spieler, das an die anvisierte Stelle in derselben Welt führt
     * (bis {@link #KURZSTRECKE_WEITE} Blöcke).
     */
    public boolean kurzstrecke(ServerPlayer player, ItemStack gun) {
        if (player.getCooldowns().isOnCooldown(this)) return false;
        ServerLevel level = player.serverLevel();
        HitResult hit = player.pick(KURZSTRECKE_WEITE, 1.0f, false);
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.riftverse.kurzstrecke_kein_ziel", KURZSTRECKE_WEITE)
                    .withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        BlockPos target = bhr.getBlockPos().relative(bhr.getDirection());
        Vec3 dest = groundBelow(level, Vec3.atBottomCenterOf(target));
        if (shoot(player, gun) == null) return false;
        Vec3 spot = groundBelow(level, player.position().add(Vec3.directionFromRotation(0f, player.getYRot()).scale(2.5)));
        RiftPortalEntity portal = RiftPortalEntity.create(level, spot, player.getYRot() + 180f, RiftPortalEntity.STYLE_RANDOM, 300);
        portal.setExactTarget(level.dimension(), dest);
        portal.setSpawnReturn(false);
        level.addFreshEntity(portal);
        effects(level, player, dest);
        player.displayClientMessage(Component.translatable("message.riftverse.kurzstrecke",
                (int) Math.round(dest.distanceTo(player.position()))).withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    /** Taste V (Mk III): nächste Auswahl; mit Schleichen das gewählte Ziel löschen. */
    public void zielWechseln(ServerPlayer player, ItemStack gun, boolean loeschen) {
        if (tier < 3) {
            player.displayClientMessage(Component.translatable("message.riftverse.ziele_erst_mk3").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        KanonenZiele z = ziele(gun);
        z = loeschen ? z.loeschen() : z.weiter();
        gun.set(ModDataComponents.KANONEN_ZIELE.get(), z);
        player.displayClientMessage(auswahlText(z).copy().withStyle(loeschen ? ChatFormatting.GOLD : ChatFormatting.AQUA), true);
    }

    public static Component auswahlText(KanonenZiele z) {
        if (z.auswahl() == KanonenZiele.ZUFALL) return Component.translatable("message.riftverse.ziel_zufall");
        if (z.auswahl() == KanonenZiele.KURZSTRECKE) return Component.translatable("message.riftverse.ziel_kurzstrecke");
        UniverseId id = z.gewaehlt();
        if (id == null) return Component.translatable("message.riftverse.ziel_zufall");
        return Component.translatable("message.riftverse.ziel_nummer", z.auswahl() + 1, z.ziele().size(),
                "Universum " + id.code(), id.type().displayName());
    }

    /** Universum der Koordinatenkarte in der anderen Hand, falls diese Kanone es erreicht. */
    @Nullable
    private UniverseId mapTarget(ServerPlayer player, ItemStack gun) {
        ItemStack other = player.getMainHandItem() == gun ? player.getOffhandItem() : player.getMainHandItem();
        if (!other.is(ModItems.KOORDINATENKARTE.get())) return null;
        UniverseId id = other.get(ModDataComponents.ZIEL.get());
        if (id == null) return null;
        if (id.type().gunTier() > tier) {
            player.displayClientMessage(Component.translatable("message.riftverse.karte_zu_weit", id.type().displayName())
                    .withStyle(ChatFormatting.RED), true);
            return null;
        }
        return id;
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
     * @return false, wenn kein passendes (unzerfallenes) Portalfluid da ist
     */
    public boolean fire(ServerPlayer player, ItemStack gun) {
        return shoot(player, gun) != null;
    }

    /** Mk I verträgt Roh-Portalfluid, ab Mk II nicht mehr. */
    public boolean acceptsRaw() {
        return accepts(1);
    }

    /**
     * Wie {@link #fire}, gibt aber zurück, womit geschossen wurde.
     * @return der Tankinhalt vor dem Schuss, oder null, wenn die Kanone nicht feuern kann
     */
    @Nullable
    public KanonenTank shoot(ServerPlayer player, ItemStack gun) {
        if (player.getAbilities().instabuild) {
            player.getCooldowns().addCooldown(this, 20);
            return KanonenTank.ofStufe(TANK, 0, Math.min(4, tier));
        }
        long now = Zerfall.now(player.level());
        KanonenTank tank = tank(gun);
        if (tank.mb() > 0 && Zerfall.zerfallen(tank.zerfall(), now)) {
            tank = KanonenTank.LEER;
            player.displayClientMessage(Component.translatable("message.riftverse.kanone_zerfallen").withStyle(ChatFormatting.GRAY), false);
        }
        if (tank.mb() > 0 && !accepts(tank.stufe())) tank = KanonenTank.LEER;
        // Set-Bonus der Multiversal-Rüstung: halber Verbrauch
        int cost = dev.riftverse.ruestung.Ruestung.kanonenKosten(player, shotCost());
        // Erst aus dem Brusttank des Kernmantels nachtanken, dann aus Kartuschen
        if (tank.mb() < cost) {
            KanonenTank brust = dev.riftverse.ruestung.Ruestung.ausBrusttank(player, this, tank, cost);
            if (brust != null) {
                tank = brust;
                player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.6f, 1.8f);
            }
        }
        if (tank.mb() < cost) {
            ItemStack cartridge = takeCartridge(player, now, this);
            if (cartridge == null) {
                gun.set(ModDataComponents.KANONEN_TANK.get(), tank);
                String key = tier >= 4 ? "message.riftverse.kein_fluid_essenz"
                        : tier >= 3 ? "message.riftverse.kein_fluid_konzentrat"
                        : acceptsRaw() ? "message.riftverse.kein_fluid" : "message.riftverse.kein_fluid_stabil";
                player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
                player.level().playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1f, 1.2f);
                return null;
            }
            int stufe = stufe(cartridge);
            // Ein Rest der anderen Sorte verpufft beim Nachladen
            int rest = tank.mb() > 0 && tank.stufe() == stufe ? tank.mb() : 0;
            long z = stufe >= 2 ? 0 : FluidKartuscheItem.zerfall(cartridge);
            tank = KanonenTank.ofStufe(Math.min(TANK, rest + FluidKartuscheItem.INHALT), rest > 0 ? Zerfall.frueher(tank.zerfall(), z) : z,
                    stufe);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.8f, 1.6f);
        }
        int left = tank.mb() - cost;
        gun.set(ModDataComponents.KANONEN_TANK.get(), left > 0
                ? new KanonenTank(left, tank.zerfall(), tank.stabil(), tank.konzentrat(), tank.essenz()) : KanonenTank.LEER);
        player.getCooldowns().addCooldown(this, 40);
        return tank;
    }

    /** Fluid-Stufe einer vollen Kartusche: 1 roh, 2 stabil, 3 Konzentrat, 4 Essenz, 0 = keine volle Kartusche. */
    public static int stufe(ItemStack cartridge) {
        if (cartridge.is(ModItems.FLUID_KARTUSCHE_ESSENZ.get())) return 4;
        if (cartridge.is(ModItems.FLUID_KARTUSCHE_KONZENTRAT.get())) return 3;
        if (cartridge.is(ModItems.FLUID_KARTUSCHE_STABIL.get())) return 2;
        if (cartridge.is(ModItems.FLUID_KARTUSCHE_VOLL.get())) return 1;
        return 0;
    }

    /**
     * Nimmt die erste passende volle, unzerfallene Kartusche aus dem Inventar und legt dafür eine leere zurück.
     */
    @Nullable
    private static ItemStack takeCartridge(ServerPlayer player, long now, PortalKanoneItem gun) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            int stufe = stufe(stack);
            if (stufe == 0 || !gun.accepts(stufe)) continue;
            boolean stabil = stufe >= 2;
            if (!stabil && FluidKartuscheItem.zerfallen(stack, now)) {
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
        KanonenTank t = tank(stack);
        return t.essenz() ? 0xC86BFF : t.konzentrat() ? 0x2AA8C0 : t.stabil() ? 0x9A6BFF : 0x39FF6A;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        KanonenTank tank = tank(stack);
        String tankKey = tank.essenz() ? "tooltip.riftverse.kanone_tank_essenz"
                : tank.konzentrat() ? "tooltip.riftverse.kanone_tank_konzentrat"
                : tank.stabil() ? "tooltip.riftverse.kanone_tank_stabil" : "tooltip.riftverse.kanone_tank";
        tooltip.add(Component.translatable(tankKey, tank.mb(), TANK, shotCost()).withStyle(ChatFormatting.GRAY));
        Level level = context.level();
        if (tank.mb() > 0 && tank.zerfall() > 0 && level != null) {
            tooltip.add(Component.translatable("tooltip.riftverse.zerfaellt_in", Zerfall.rest(tank.zerfall(), level.getGameTime()))
                    .withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("tooltip.riftverse.kanone_mk" + tier).withStyle(ChatFormatting.DARK_GREEN));
        if (tier >= 2) tooltip.add(Component.translatable("tooltip.riftverse.kanone_karte").withStyle(ChatFormatting.DARK_GREEN));
        if (tier >= 3) {
            KanonenZiele z = ziele(stack);
            tooltip.add(auswahlText(z).copy().withStyle(ChatFormatting.AQUA));
            for (int i = 0; i < z.ziele().size(); i++) {
                UniverseId id = z.ziele().get(i);
                tooltip.add(Component.literal((i == z.auswahl() ? "▶ " : "  ") + (i + 1) + ". Universum " + id.code() + " · ")
                        .append(id.type().displayName()).withStyle(i == z.auswahl() ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
            }
            tooltip.add(Component.translatable("tooltip.riftverse.kanone_ziele").withStyle(ChatFormatting.DARK_GREEN));
        }
        tooltip.add(Component.translatable("tooltip.riftverse.kanone_bedienung").withStyle(ChatFormatting.DARK_GRAY));
    }
}
