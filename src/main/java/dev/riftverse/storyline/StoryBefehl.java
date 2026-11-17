package dev.riftverse.storyline;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.riftverse.Riftverse;
import dev.riftverse.portal.Heimanker;
import dev.riftverse.portal.RiftPortalEntity;
import dev.riftverse.registry.ModConditions;
import dev.riftverse.universe.Einzigartig;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Testbefehle für Storylines (Berechtigungsstufe 2):
 * <ul>
 *     <li>{@code /riftverse story status} – alle Storylines mit Schritt, Flags und kaputten Dateien</li>
 *     <li>{@code /riftverse story schritt <storyline> <schritt>} – direkt zu einem Schritt springen</li>
 *     <li>{@code /riftverse story flag <storyline> <flag> <an>} – Flag setzen oder löschen</li>
 *     <li>{@code /riftverse story zuruecksetzen <storyline>} – Storyline von vorn</li>
 *     <li>{@code /riftverse testwelt} – Rift in den Prüfstein (nur mit {@code entwicklung.testwelten})</li>
 * </ul>
 * Bei Storylines mit Geltung „spieler“ wirken sie auf den Stand des Spielers, der den Befehl gibt.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class StoryBefehl {
    private static final SuggestionProvider<CommandSourceStack> STORYLINES = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(StoryLoader.alle().stream().map(Storyline::id), builder);
    private static final SuggestionProvider<CommandSourceStack> SCHRITTE = (ctx, builder) -> {
        Storyline s = StoryLoader.get(ResourceLocationArgument.getId(ctx, "storyline"));
        return SharedSuggestionProvider.suggest(s == null ? java.util.stream.Stream.empty() : s.schritte().keySet().stream(), builder);
    };

    private StoryBefehl() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("riftverse")
                .then(Commands.literal("story")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("status").executes(StoryBefehl::status))
                        .then(Commands.literal("schritt")
                                .then(Commands.argument("storyline", ResourceLocationArgument.id()).suggests(STORYLINES)
                                        .then(Commands.argument("schritt", StringArgumentType.word()).suggests(SCHRITTE)
                                                .executes(StoryBefehl::schritt))))
                        .then(Commands.literal("flag")
                                .then(Commands.argument("storyline", ResourceLocationArgument.id()).suggests(STORYLINES)
                                        .then(Commands.argument("flag", StringArgumentType.string())
                                                .then(Commands.argument("an", BoolArgumentType.bool()).executes(StoryBefehl::flag)))))
                        .then(Commands.literal("zuruecksetzen")
                                .then(Commands.argument("storyline", ResourceLocationArgument.id()).suggests(STORYLINES)
                                        .executes(StoryBefehl::zuruecksetzen))))
                .then(Commands.literal("testwelt")
                        .requires(src -> src.hasPermission(2))
                        .executes(StoryBefehl::testwelt)));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (StoryLoader.alle().isEmpty()) {
            src.sendSuccess(() -> Component.translatable("message.riftverse.story.befehl.status_leer"), false);
        }
        ServerPlayer spieler = src.getPlayer();
        for (Storyline s : StoryLoader.alle()) {
            StoryStand st = s.geltung() == Storyline.Geltung.SPIELER && spieler != null
                    ? StoryManager.spielerStand(src.getServer(), s, spieler.getUUID())
                    : StoryManager.weltStand(src.getServer(), s);
            String stand = st.fertig ? "fertig" : (st.schritt == null ? "nicht begonnen (Start: " + s.start() + ")" : "Schritt " + st.schritt);
            src.sendSuccess(() -> Component.literal(s.titel() + " ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("(" + s.id() + ", " + s.welt().id() + ", " + s.geltung().name().toLowerCase() + ")")
                            .withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("\n  " + stand + " · Flags " + st.flags + " · Notizen " + st.notizen)
                            .withStyle(ChatFormatting.GRAY)), false);
        }
        if (!StoryLoader.fehler().isEmpty()) {
            src.sendFailure(Component.translatable("message.riftverse.story.befehl.fehler", String.join("; ", StoryLoader.fehler())));
        }
        return StoryLoader.alle().size();
    }

    private static int schritt(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Storyline s = storyline(ctx);
        if (s == null) return 0;
        String schritt = StringArgumentType.getString(ctx, "schritt");
        if (s.schritt(schritt) == null) {
            ctx.getSource().sendFailure(Component.translatable("message.riftverse.story.befehl.kein_schritt", schritt));
            return 0;
        }
        StoryStand st = stand(ctx, s);
        StoryManager.schrittSetzen(ctx.getSource().getServer(), s, st, schritt);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.riftverse.story.befehl.schritt", s.titel(), schritt)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int flag(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Storyline s = storyline(ctx);
        if (s == null) return 0;
        String flag = StringArgumentType.getString(ctx, "flag");
        boolean an = BoolArgumentType.getBool(ctx, "an");
        StoryStand st = stand(ctx, s);
        if (an) st.flags.add(flag);
        else st.flags.remove(flag);
        dev.riftverse.universe.MultiverseData.get(ctx.getSource().getServer()).setDirty();
        ctx.getSource().sendSuccess(() -> Component.translatable("message.riftverse.story.befehl.flag", s.titel(), flag, an ? "an" : "aus")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int zuruecksetzen(CommandContext<CommandSourceStack> ctx) {
        Storyline s = storyline(ctx);
        if (s == null) return 0;
        StoryManager.zuruecksetzen(ctx.getSource().getServer(), s);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.riftverse.story.befehl.zurueck", s.titel())
                .withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int testwelt(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        UniverseType test = UniverseType.TEST_EINZIGARTIG;
        if (!ModConditions.entwicklung() || player.server.getLevel(test.dimension()) == null) {
            ctx.getSource().sendFailure(Component.translatable("message.riftverse.testwelt.aus"));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        if (Multiverse.at(level, player.blockPosition()) == null) Heimanker.set(player, level.dimension(), player.position());
        UniverseId ziel = Einzigartig.id(test);
        Vec3 spot = player.position().add(Vec3.directionFromRotation(0f, player.getYRot()).scale(2.5));
        RiftPortalEntity portal = RiftPortalEntity.create(level, spot, player.getYRot() + 180f, RiftPortalEntity.STYLE_RANDOM, 600);
        portal.setUniverseTarget(test.dimension(), ziel);
        portal.setSpawnReturn(false);
        level.addFreshEntity(portal);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.riftverse.testwelt.offen").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    @Nullable
    private static Storyline storyline(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "storyline");
        Storyline s = StoryLoader.get(id);
        if (s == null) ctx.getSource().sendFailure(Component.translatable("message.riftverse.story.befehl.keine", id.toString()));
        return s;
    }

    private static StoryStand stand(CommandContext<CommandSourceStack> ctx, Storyline s) throws CommandSyntaxException {
        if (s.geltung() == Storyline.Geltung.SPIELER) {
            return StoryManager.spielerStand(ctx.getSource().getServer(), s, ctx.getSource().getPlayerOrException().getUUID());
        }
        return StoryManager.weltStand(ctx.getSource().getServer(), s);
    }
}
