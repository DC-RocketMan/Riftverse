package dev.riftverse.client.story;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riftverse.storyline.StoryFigur;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Story-Figur mit Spielermodell. Läuft gerade ein Dialog mit ihr, schwebt er als Tafel über ihrem Kopf: Sprecher,
 * Text, Antworten (1–4) und ein Hinweis zum Weiterklicken. Nur der Spieler im Gespräch sieht ihn.
 */
public class StoryFigurRenderer extends HumanoidMobRenderer<StoryFigur, PlayerModel<StoryFigur>> {
    private static final int BREITE = 200;
    private static final int HINTERGRUND = 0xB0000000;
    private static final Map<String, ResourceLocation> TEXTUREN = new HashMap<>();

    /** Figuren mit {@code "aussehen": "gleeb:<beruf>"} sehen aus wie Gleebs aus Bridge's Inn. */
    private final dev.riftverse.client.dorf.GleebRenderer<StoryFigur> gleeb;

    public StoryFigurRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.gleeb = new dev.riftverse.client.dorf.GleebRenderer<>(context, StoryFigurRenderer::beruf);
    }

    public static boolean istGleeb(StoryFigur figur) {
        return figur.aussehen().startsWith("gleeb");
    }

    private static dev.riftverse.dorf.Beruf beruf(StoryFigur figur) {
        String a = figur.aussehen();
        return a.startsWith("gleeb:") ? dev.riftverse.dorf.Beruf.von(a.substring(6)) : dev.riftverse.dorf.Beruf.BUERGER;
    }

    @Override
    public ResourceLocation getTextureLocation(StoryFigur figur) {
        if (istGleeb(figur)) return dev.riftverse.client.dorf.GleebRenderer.textur(beruf(figur));
        return TEXTUREN.computeIfAbsent(figur.aussehen(), StoryFigurRenderer::textur);
    }

    /** {@code steve}, {@code alex} … (Standard-Skins) oder ein vollständiger Texturpfad. */
    private static ResourceLocation textur(String aussehen) {
        if (!aussehen.contains(":") && !aussehen.contains("/")) {
            return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/" + aussehen + ".png");
        }
        ResourceLocation r = ResourceLocation.tryParse(aussehen);
        return r != null ? r : ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    }

    @Override
    public void render(StoryFigur figur, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        if (istGleeb(figur)) gleeb.render(figur, yaw, partialTicks, pose, buffers, light);
        else super.render(figur, yaw, partialTicks, pose, buffers, light);
        ClientStory.Anzeige a = ClientStory.fuer(figur);
        if (a != null) dialogZeichnen(figur, a, pose, buffers);
    }

    private void dialogZeichnen(StoryFigur figur, ClientStory.Anzeige a, PoseStack pose, MultiBufferSource buffers) {
        Font font = getFont();
        List<FormattedCharSequence> zeilen = new ArrayList<>();
        List<Integer> farben = new ArrayList<>();
        zeilen.add(Component.literal(a.sprecher()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD).getVisualOrderText());
        farben.add(0xFFFFFFFF);
        for (FormattedCharSequence z : font.split(Component.literal(a.text()), BREITE)) {
            zeilen.add(z);
            farben.add(0xFFFFFFFF);
        }
        for (int i = 0; i < a.antworten().size(); i++) {
            for (FormattedCharSequence z : font.split(Component.literal((i + 1) + "  " + a.antworten().get(i)), BREITE)) {
                zeilen.add(z);
                farben.add(0xFF7FE8FF);
            }
        }
        Component hinweis = a.antworten().isEmpty()
                ? (a.mehr() ? Component.translatable("dialog.riftverse.weiter") : null)
                : Component.translatable("dialog.riftverse.antworten");
        if (hinweis != null) {
            zeilen.add(hinweis.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC).getVisualOrderText());
            farben.add(0xFFAAAAAA);
        }

        int breite = 0;
        for (FormattedCharSequence z : zeilen) breite = Math.max(breite, font.width(z));

        pose.pushPose();
        pose.translate(0.0, (istGleeb(figur) ? 1.05 : figur.getBbHeight()) + 0.75, 0.0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.scale(0.022f, -0.022f, 0.022f);
        Matrix4f m = pose.last().pose();
        float y = -zeilen.size() * 10f;
        for (int i = 0; i < zeilen.size(); i++) {
            FormattedCharSequence z = zeilen.get(i);
            float x = -breite / 2f;
            font.drawInBatch(z, x, y, farben.get(i), false, m, buffers, Font.DisplayMode.NORMAL, HINTERGRUND, LightTexture.FULL_BRIGHT);
            y += 10f;
        }
        pose.popPose();
    }
}
