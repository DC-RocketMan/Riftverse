package dev.riftverse.client.dorf;

import dev.riftverse.Riftverse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Verhühnerte Spieler sehen für alle wie Hühner aus (M10-14): Statt des Spielermodells wird ein Huhn gezeichnet,
 * das Laufen und Flattern des Spielers nachmacht. Die eigene Hand verschwindet.
 */
@EventBusSubscriber(modid = Riftverse.MODID, value = Dist.CLIENT)
public final class HuhnRender {
    /** Entity-ID → Spielzeit, bis zu der der Spieler ein Huhn ist. */
    private static final Map<Integer, Long> BIS = new HashMap<>();
    private static final Map<Integer, Chicken> HUEHNER = new HashMap<>();

    private HuhnRender() {
    }

    public static void status(int entity, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (ticks <= 0) {
            BIS.remove(entity);
            HUEHNER.remove(entity);
        } else {
            BIS.put(entity, mc.level.getGameTime() + ticks);
        }
    }

    public static boolean ist(Player p) {
        Long bis = BIS.get(p.getId());
        return bis != null && p.level().getGameTime() < bis;
    }

    private static Chicken huhn(Player p) {
        return HUEHNER.computeIfAbsent(p.getId(), id -> new Chicken(EntityType.CHICKEN, p.level()));
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player p = event.getEntity();
        if (!ist(p)) return;
        event.setCanceled(true);
        Chicken c = huhn(p);
        c.setPos(p.getX(), p.getY(), p.getZ());
        c.yBodyRot = p.yBodyRot;
        c.yBodyRotO = p.yBodyRotO;
        c.yHeadRot = p.yHeadRot;
        c.yHeadRotO = p.yHeadRotO;
        c.setXRot(p.getXRot());
        c.xRotO = p.xRotO;
        c.setYRot(p.getYRot());
        c.yRotO = p.yRotO;
        c.setOnGround(p.onGround());
        c.tickCount = p.tickCount;
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.scale(1.4f, 1.4f, 1.4f);
        EntityRenderer<? super Chicken> r = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(c);
        r.render(c, p.getYRot(), event.getPartialTick(), pose, event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Player p = Minecraft.getInstance().player;
        if (p != null && ist(p)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || HUEHNER.isEmpty()) return;
        HUEHNER.entrySet().removeIf(e -> {
            if (!(mc.level.getEntity(e.getKey()) instanceof Player p) || !ist(p)) return true;
            Chicken c = e.getValue();
            double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
            float tempo = (float) Math.min(1.0, Math.sqrt(dx * dx + dz * dz) * 4.0);
            c.walkAnimation.update(tempo, 0.4f);
            c.oFlap = c.flap;
            c.oFlapSpeed = c.flapSpeed;
            c.flapSpeed += (p.onGround() ? -1.0f : 4.0f) * 0.3f;
            c.flapSpeed = Math.max(0f, Math.min(1f, c.flapSpeed));
            if (!p.onGround() && c.flapping < 1.0f) c.flapping = 1.0f;
            c.flapping *= 0.9f;
            c.flap += c.flapping * 2.0f;
            return false;
        });
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        BIS.clear();
        HUEHNER.clear();
    }
}
