package dev.riftverse.client.handbuch;

import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseType;
import guideme.compiler.PageCompiler;
import guideme.compiler.tags.BlockTagCompiler;
import guideme.document.block.LytBlockContainer;
import guideme.document.block.LytParagraph;
import guideme.document.block.table.LytTable;
import guideme.document.block.table.LytTableRow;
import guideme.libs.mdast.mdx.model.MdxJsxElementFields;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Handbuch-Tag {@code <RiftChancen />}: Tabelle, wohin ein Zufallssprung mit welcher Chance führt, je Kanonen- bzw.
 * Fluidstufe. Die Werte kommen direkt aus den Gewichten der Universumstypen (inklusive Config), deshalb veraltet die
 * Tabelle nie.
 */
public class RiftChancenCompiler extends BlockTagCompiler {
    private static final String[] STUFEN = {"Mk I", "Mk II", "Mk III", "Mk IV"};

    @Override
    public Set<String> getTagNames() {
        return Set.of("RiftChancen");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytTable table = new LytTable();
        table.setMarginBottom(5);

        LytTableRow kopf = table.appendRow();
        kopf.modifyStyle(style -> style.bold(true));
        zelle(kopf, "Universum");
        for (String stufe : STUFEN) zelle(kopf, stufe);

        for (UniverseType typ : UniverseType.randomWith(STUFEN.length)) {
            LytTableRow row = table.appendRow();
            String name = Component.translatable(typ.translationKey()).getString();
            zelle(row, typ.einzigartig() ? name + " ✦" : name);
            for (int stufe = 1; stufe <= STUFEN.length; stufe++) {
                List<UniverseType> erreichbar = UniverseType.randomWith(stufe);
                zelle(row, erreichbar.contains(typ) ? prozent(Multiverse.chance(typ, erreichbar)) : "–");
            }
        }
        parent.append(table);
    }

    private static String prozent(double chance) {
        return String.format(Locale.GERMAN, "%.1f %%", chance * 100);
    }

    private static void zelle(LytTableRow row, String text) {
        row.appendCell().append(LytParagraph.of(text));
    }
}
