package dev.riftverse.universe;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.riftverse.Riftverse;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;

/**
 * Befehl für Server-Admins (Berechtigungsstufe 3):
 * <ul>
 *     <li>{@code /riftverse einzigartig zuruecksetzen <welt>} – fragt nach</li>
 *     <li>{@code /riftverse einzigartig zuruecksetzen <welt> bestaetigen} – merkt das Zurücksetzen vor</li>
 *     <li>{@code /riftverse einzigartig abbrechen <welt>} – nimmt die Vormerkung zurück</li>
 * </ul>
 * Zurückgesetzt wird beim nächsten Serverstart, weil die Region-Dateien im laufenden Betrieb offen sind. Danach
 * beginnen Gelände, Bauten, Story-Stand und die ersten Ankünfte aller Spieler von vorn.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class EinzigartigBefehl {
    private EinzigartigBefehl() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("riftverse")
                .then(Commands.literal("einzigartig")
                        .requires(src -> src.hasPermission(3))
                        .then(Commands.literal("zuruecksetzen")
                                .then(Commands.argument("welt", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(UniverseType.values()).filter(UniverseType::einzigartig)
                                                        .map(UniverseType::id), builder))
                                        .executes(ctx -> zuruecksetzen(ctx, false))
                                        .then(Commands.literal("bestaetigen").executes(ctx -> zuruecksetzen(ctx, true)))))
                        .then(Commands.literal("abbrechen")
                                .then(Commands.argument("welt", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(UniverseType.values()).filter(UniverseType::einzigartig)
                                                        .map(UniverseType::id), builder))
                                        .executes(EinzigartigBefehl::abbrechen)))));
    }

    private static int zuruecksetzen(CommandContext<CommandSourceStack> ctx, boolean bestaetigt) {
        UniverseType type = welt(ctx);
        if (type == null) return 0;
        UniverseId id = Einzigartig.id(type);
        if (!bestaetigt) {
            ctx.getSource().sendSuccess(() -> Component.translatable("message.riftverse.befehl.zuruecksetzen.frage",
                    id.label(), "/riftverse einzigartig zuruecksetzen " + type.id() + " bestaetigen")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        MultiverseData data = MultiverseData.get(ctx.getSource().getServer());
        data.record(id).resetAngefordert = true;
        data.setDirty();
        ctx.getSource().sendSuccess(() -> Component.translatable("message.riftverse.befehl.zuruecksetzen.ok", id.label())
                .withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int abbrechen(CommandContext<CommandSourceStack> ctx) {
        UniverseType type = welt(ctx);
        if (type == null) return 0;
        UniverseId id = Einzigartig.id(type);
        MultiverseData data = MultiverseData.get(ctx.getSource().getServer());
        MultiverseData.Record rec = data.existing(id);
        if (rec == null || !rec.resetAngefordert) {
            ctx.getSource().sendFailure(Component.translatable("message.riftverse.befehl.abbrechen.nichts", id.label()));
            return 0;
        }
        rec.resetAngefordert = false;
        data.setDirty();
        ctx.getSource().sendSuccess(() -> Component.translatable("message.riftverse.befehl.abbrechen.ok", id.label())
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static UniverseType welt(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "welt");
        UniverseType type = UniverseType.byId(name);
        if (type == null || !type.einzigartig()) {
            ctx.getSource().sendFailure(Component.translatable("message.riftverse.befehl.keine_einzigartige_welt", name));
            return null;
        }
        return type;
    }
}
