package dev.riftverse.dorf;

import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModDorf;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Die Huhn-Strafe von Bridge's Inn: Wer einen Gleeb schlägt, wird verhühnert. Ein Huhn kann nichts abbauen,
 * nichts setzen, nichts benutzen und niemanden schlagen. Alle Spieler in der Nähe sehen es als Huhn – dafür
 * schickt der Server ein {@link Status}-Paket an alle, die den Spieler sehen.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class Huhn {
    private Huhn() {
    }

    public static boolean ist(Player player) {
        return player.hasEffect(ModDorf.VERHUEHNERT);
    }

    /** Verhühnert den Spieler (oder verlängert es) und lässt den Gleeb lachen. */
    public static void verhuehnern(ServerPlayer player, net.minecraft.world.entity.Entity lacher) {
        boolean schon = ist(player);
        player.addEffect(new MobEffectInstance(ModDorf.VERHUEHNERT, VerhuehnertEffect.DAUER, 0, false, true, true));
        ServerLevel level = player.serverLevel();
        level.playSound(null, lacher.getX(), lacher.getY(), lacher.getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.NEUTRAL, 1.2f, 1.3f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHICKEN_HURT, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.6, 0.4, 0.1);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.8, player.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, lacher.getX(), lacher.getY() + 1.4, lacher.getZ(), 8, 0.3, 0.3, 0.3, 0);
        if (!schon) player.displayClientMessage(Component.translatable("dorf.riftverse.verhuehnert"), true);
        senden(player, VerhuehnertEffect.DAUER);
    }

    private static void senden(ServerPlayer player, int ticks) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new Status(player.getId(), ticks));
    }

    // ------------------------------------------------------------------ Was ein Huhn nicht kann

    private static void sperren(Player player, ICancellableEvent event) {
        if (player != null && ist(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        sperren(event.getPlayer(), event);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player p) sperren(p, event);
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        sperren(event.getEntity(), event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (ist(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (ist(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        sperren(event.getEntity(), event);
    }

    // ------------------------------------------------------------------ Anzeige bei den anderen

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer ziel && event.getEntity() instanceof ServerPlayer sieht) {
            MobEffectInstance e = ziel.getEffect(ModDorf.VERHUEHNERT);
            if (e != null) PacketDistributor.sendToPlayer(sieht, new Status(ziel.getId(), e.getDuration()));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            MobEffectInstance e = p.getEffect(ModDorf.VERHUEHNERT);
            if (e != null) senden(p, e.getDuration());
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            MobEffectInstance e = p.getEffect(ModDorf.VERHUEHNERT);
            if (e != null) senden(p, e.getDuration());
        }
    }

    @SubscribeEvent
    public static void onEffektWeg(MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getEffect().is(ModDorf.VERHUEHNERT.getKey())) {
            senden(p, 0);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CHICKEN_DEATH, SoundSource.PLAYERS, 0.8f, 1.4f);
        }
    }

    @SubscribeEvent
    public static void onEffektAbgelaufen(MobEffectEvent.Expired event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getEffectInstance() != null
                && event.getEffectInstance().getEffect().is(ModDorf.VERHUEHNERT.getKey())) {
            senden(p, 0);
            p.displayClientMessage(Component.translatable("dorf.riftverse.entheuhnert"), true);
        }
    }

    // ------------------------------------------------------------------ Netzwerk

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Status.TYPE, Status.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> dev.riftverse.client.dorf.HuhnRender.status(p.entity(), p.ticks())));
    }

    /** Spieler {@code entity} ist noch {@code ticks} Ticks ein Huhn (0 = nicht mehr). */
    public record Status(int entity, int ticks) implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(Riftverse.id("verhuehnert"));
        public static final StreamCodec<ByteBuf, Status> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Status::entity, ByteBufCodecs.VAR_INT, Status::ticks, Status::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
