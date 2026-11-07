package dev.riftverse.story;

import dev.riftverse.boss.BossAbility;
import dev.riftverse.universe.UniverseProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Der Story-Generator. Erzeugt Handlungen, Dialoge und Epiloge in Echtzeit aus
 * Bausteinen: Persönlichkeit × Ereignis × Kontext (Physik des Universums,
 * Lebenspunkte, Tode, gehaltenes Item, Kampfdauer) × Zufall.
 * Kein externes KI-Modell nötig – läuft komplett im Mod.
 */
public final class StoryEngine {

    public enum Event { GREET, FIGHT, HURT, PHASE2, PHASE3, KILL, BEG, BEG_REFUSED, SPARED, DEFEAT, DEFEAT_BEGGING, IDLE }

    public enum Outcome { DEFEATED, SPARED, EXECUTED }

    /** Momentaufnahme der Situation, aus der ein Satz gebaut wird. */
    public record Ctx(String player, float playerHealth, float bossHealth, @Nullable UniverseProfile universe,
                      int deaths, String heldItem, int fightSeconds) {}

    // ================================================================== Bausteine

    private static final String[] TITLES = {
            "der Unvermeidliche", "der Dreiundzwanzigste", "Kanzler von {u}", "Oberster Quanten-Bürokrat",
            "Erbe von {h}", "die Singularität auf zwei Beinen", "der Gravitationsflüsterer",
            "Großmeister der Paradoxa", "Fürst der Fallhöhen", "die Stimme aus dem Rift",
            "Verwalter der Naturkonstanten", "der Letzte seiner Zeitlinie", "Hüter vom {a}",
            "Generaldirektor des Chaos", "der Zweimal-Gestorbene"};

    private static final String[] MOTIVES = {
            "will {u} mit einer Steuer auf Schwerkraft belegen",
            "hat das {a} gestohlen und will damit alle Rifts im Multiversum versiegeln",
            "plant, das {a} zu zünden und {u} in eine Pfütze aus Möglichkeiten zu verwandeln",
            "züchtet {m} und will mit ihnen die Nachbaruniversen annektieren",
            "hält die Bewohner von {u} für eine schlecht programmierte Simulation und will sie löschen",
            "hat die Naturkonstanten privatisiert und vermietet jetzt Sauerstoff",
            "sucht die Version von {p} aus diesem Zeitstrang, um sie zu ersetzen",
            "baut aus dem {a} eine Waffe, die ganze Universen zusammenfalten kann",
            "will {h} wiederaufbauen – mit den Trümmern von {u}"};

    private static final String[] HOOK_OPENERS = {
            "Hallo? Funktioniert das Ding?", "An den Fremden mit der leuchtenden Kanone:", "Bitte hilf uns, Reisender!",
            "Endlich ein Rift! Hör mir gut zu:", "Falls das hier jemand empfängt:", "Notruf, Priorität Omega:"};

    private static final String[] HOOK_PLEAS = {
            "Du bist unsere einzige Hoffnung. Oder zumindest die Einzige, die gerade da ist.",
            "Wir können nicht viel zahlen, aber {b} hat eine Menge Portal-Fluid gehortet.",
            "Beeil dich – und bitte bring nicht noch eine Version von dir mit.",
            "Wenn du {b} aufhältst, schreiben wir ein Lied über dich. Ein kurzes.",
            "Man sagt, {b} hat eine Schwachstelle. Wir wissen nur nicht, welche."};

    private static final String[] RETURNING = {
            "Schon wieder du? Versuch Nummer {n1}, {p}. Ich zähle mit.",
            "Du lernst nicht dazu, oder? Das ist dein {n1}. Anlauf.",
            "Ah, {p}. Ich habe deinen letzten Abgang noch in Zeitlupe gespeichert."};

    private static final String[] PLAYER_LOW = {
            "Du blutest, {p}.", "Nur noch ein paar Herzen? Wie poetisch.", "Deine Knie zittern, {p}. Ich höre es.",
            "Soll ich langsamer machen? Nein? Gut."};

    private static final String[] ITEM_REMARKS = {
            "Mit {item}? Ernsthaft?", "Schöne {item}. Bekommt sie einen Ehrenplatz in meiner Sammlung?",
            "Ich hatte auch mal {item}. Dann bin ich erwachsen geworden."};

    private static final String[] LONG_FIGHT = {
            "Wir tanzen jetzt schon {sec} Sekunden. Ich werde müde – du auch?",
            "{sec} Sekunden! In {h} wäre das schon ein Feiertag.",
            "Weißt du, wie viele Universen in {sec} Sekunden entstehen? Viele. Und keins mag dich."};

    private static final Map<Event, String[]> GENERIC = new EnumMap<>(Event.class);
    private static final Map<Personality, Map<Event, String[]>> LINES = new EnumMap<>(Personality.class);

    private static void put(Personality p, Event e, String... lines) {
        LINES.computeIfAbsent(p, k -> new EnumMap<>(Event.class)).put(e, lines);
    }

    static {
        GENERIC.put(Event.GREET, new String[]{"Du bist also {p}. Ich dachte, du wärst größer.", "Willkommen in {u}. Du wirst nicht bleiben."});
        GENERIC.put(Event.FIGHT, new String[]{"Dann los!", "Zeig, was du kannst, {p}!"});
        GENERIC.put(Event.HURT, new String[]{"Hmpf.", "Das merke ich mir."});
        GENERIC.put(Event.PHASE2, new String[]{"Jetzt wird es ernst."});
        GENERIC.put(Event.PHASE3, new String[]{"Letzte Runde, {p}!"});
        GENERIC.put(Event.KILL, new String[]{"Einer weniger."});
        GENERIC.put(Event.BEG, new String[]{"Warte! Lass mich gehen – ein Rechtsklick genügt!"});
        GENERIC.put(Event.BEG_REFUSED, new String[]{"Gut. Dann eben auf die harte Tour!", "Keine Antwort ist auch eine Antwort. Eine dumme!",
                "Zeit abgelaufen, {p}!"});
        GENERIC.put(Event.SPARED, new String[]{"Danke. Ich werde es nicht vergessen."});
        GENERIC.put(Event.DEFEAT, new String[]{"Unmöglich…"});
        GENERIC.put(Event.DEFEAT_BEGGING, new String[]{"Ich… hatte doch… gebettelt…", "Du hättest… nur… klicken müssen…"});
        GENERIC.put(Event.IDLE, new String[]{"Versteckst du dich, {p}?", "Komm raus, komm raus, wo immer du bist!",
                "Ich kann warten. Ich habe {h} überlebt."});

        // ---------------- Größenwahn
        put(Personality.MEGALOMANIAC, Event.GREET,
                "Ah, {p}. Ich habe dein Universum schon einmal zusammengefaltet – du warst damals kleiner.",
                "Knie nieder vor {b} {t}! Oder steh rum, das Ergebnis ist dasselbe.",
                "Noch ein Rift-Tourist. Willkommen in MEINEM {u}.");
        put(Personality.MEGALOMANIAC, Event.FIGHT,
                "Du wagst es? Ich habe Galaxien für weniger gelöscht!",
                "Gut. Ich brauchte sowieso ein neues Kapitel für meine Memoiren.");
        put(Personality.MEGALOMANIAC, Event.HURT,
                "Das war ein Kratzer. Ein historischer Kratzer, aber ein Kratzer.",
                "Du hast mich getroffen?! Das wird aus den Geschichtsbüchern gestrichen!",
                "Autsch. Ich meine: bedeutungslos.");
        put(Personality.MEGALOMANIAC, Event.PHASE2,
                "Genug gespielt. Jetzt zeige ich dir, warum man mich in sieben Dimensionen fürchtet!",
                "Du zwingst mich, mich anzustrengen. Das ist unhöflich.");
        put(Personality.MEGALOMANIAC, Event.PHASE3,
                "Unmöglich! Ich bin {b} {t}! Ich BIN das {a}!",
                "Wenn ich falle, nehme ich dieses ganze Universum mit!");
        put(Personality.MEGALOMANIAC, Event.KILL,
                "Und wieder ein {p} weniger im Multiversum. Es gibt ja genug von euch.",
                "Notiert: {p}, Versuch {n}. Ergebnis: vorhersehbar.");
        put(Personality.MEGALOMANIAC, Event.BEG,
                "Warte! Ein Herrscher braucht einen Stellvertreter. Lass mich gehen – ein Rechtsklick – und du bekommst einen Titel!");
        put(Personality.MEGALOMANIAC, Event.SPARED,
                "Gnade?! Du hast mich gedemütigt. Das vergesse ich nie. Danke. Nein! Doch. Danke.");
        put(Personality.MEGALOMANIAC, Event.DEFEAT,
                "Das… war nicht… im Plan…", "Mein Reich… mein {a}… mein wunderschönes Ego…");

        // ---------------- Bürokrat
        put(Personality.BUREAUCRAT, Event.GREET,
                "Halt! Haben Sie das Formular 7-B zur interdimensionalen Einreise ausgefüllt, {p}?",
                "Nummer ziehen, bitte. Ihre Vernichtung wird gleich bearbeitet.",
                "Gemäß Paragraph 12 des {u}-Kodex sind Sie hier illegal. Bitte bleiben Sie stehen, während ich Sie zerlege.");
        put(Personality.BUREAUCRAT, Event.FIGHT,
                "Widerstand gegen die Verwaltung! Das gibt eine Mahngebühr!",
                "Ich muss Sie leider zu den Akten legen. Dauerhaft.");
        put(Personality.BUREAUCRAT, Event.HURT,
                "Das ist Sachbeschädigung an einem Beamten!",
                "Autsch. Ich reiche Beschwerde ein. In dreifacher Ausfertigung.",
                "Notiert. Abgestempelt. Gerächt wird später.");
        put(Personality.BUREAUCRAT, Event.PHASE2,
                "Ich eskaliere das jetzt an die nächsthöhere Instanz: mich, aber wütend.",
                "Die Bearbeitungszeit verkürzt sich. Für SIE.");
        put(Personality.BUREAUCRAT, Event.PHASE3,
                "Ausnahmezustand! Alle Formulare sind ab sofort Waffen!",
                "Ich habe einen Antrag auf Ihr Ende gestellt – und er wurde genehmigt!");
        put(Personality.BUREAUCRAT, Event.KILL,
                "Vorgang {p} abgeschlossen. Nächster!",
                "Ihre Akte wird mit dem Vermerk ‚verstorben‘ geschlossen. Gebühr: ein Leben.");
        put(Personality.BUREAUCRAT, Event.BEG,
                "Moment! Das können wir doch auf dem Dienstweg regeln! Ein Rechtsklick und ich stelle Ihnen einen Passierschein aus!",
                "Ich beantrage hiermit Gnade! Formular G-1! Bitte unterschreiben Sie – einfach mit einem Rechtsklick!");
        put(Personality.BUREAUCRAT, Event.SPARED,
                "Antrag auf Gnade bewilligt. Hier, Ihre Unterlagen. Ich versetze mich selbst in ein anderes Universum.");
        put(Personality.BUREAUCRAT, Event.DEFEAT,
                "Mein… Stempel… er ist… ausgetrocknet…", "Bitte… legen Sie… Widerspruch ein… für mich…");

        // ---------------- Nihilist
        put(Personality.NIHILIST, Event.GREET,
                "Du bist durch ein Loch im Universum gestiegen, {p}, nur um mich zu sehen. Was sagt das über dich?",
                "Nichts hat Bedeutung. Außer vielleicht dieser Kampf. Nein, der auch nicht.",
                "Weißt du, wie viele Versionen von dir ich schon gesehen habe? Jede dachte, sie sei die Echte.");
        put(Personality.NIHILIST, Event.FIGHT,
                "Gut. Lass uns gemeinsam die Entropie beschleunigen.",
                "Kampf ist nur Physik mit schlechter Laune.");
        put(Personality.NIHILIST, Event.HURT,
                "Schmerz. Interessant. Ich hatte vergessen, wie er sich anfühlt.",
                "Du triffst mich, also existiere ich. Danke, glaube ich.");
        put(Personality.NIHILIST, Event.PHASE2,
                "Ich beginne, mich für das Ergebnis zu interessieren. Das ist dein Fehler.",
                "Vielleicht bedeutet doch etwas etwas. Zum Beispiel dein Ende.");
        put(Personality.NIHILIST, Event.PHASE3,
                "Wenn alles egal ist, ist es auch egal, wie sehr ich jetzt eskaliere.",
                "Das {a} flüstert mir zu. Es sagt: keine Gnade. Es ist ein sehr schlichtes Artefakt.");
        put(Personality.NIHILIST, Event.KILL,
                "Du bist jetzt Teil des großen Nichts, {p}. Grüß es von mir.",
                "Und? War es das wert? War irgendetwas das wert?");
        put(Personality.NIHILIST, Event.BEG,
                "Vielleicht ist Weiterleben auch nur eine Möglichkeit unter vielen. Lässt du mich gehen, {p}? Ein Rechtsklick.");
        put(Personality.NIHILIST, Event.SPARED,
                "Du verschonst mich. Das ist entweder Güte oder Gleichgültigkeit. Beides respektiere ich.");
        put(Personality.NIHILIST, Event.DEFEAT,
                "Ah. Das Nichts. Endlich ein Ort ohne Smalltalk.", "Es war… alles… nur Zufall…");

        // ---------------- Durchgeknallt
        put(Personality.MANIAC, Event.GREET,
                "{p}! {p}! Du bist ECHT! Ich dachte, du bist eine meiner Halluzinationen! Egal, ich bekämpf dich trotzdem!",
                "Ich habe dieses Universum einmal umgedreht und es ist so geblieben! Hihi!",
                "Willkommen! Wir haben Kuchen! Der Kuchen ist eine Falle! Die Falle ist auch ein Kuchen!");
        put(Personality.MANIAC, Event.FIGHT,
                "JAAA! Endlich passiert mal was!",
                "Ich hab die Physik hier mit einem Löffel kalibriert! Mal sehen, was passiert!");
        put(Personality.MANIAC, Event.HURT,
                "Au! Mach das nochmal! Nein, nicht!", "Hahaha, das kitzelt! Und blutet!",
                "Du hast meine Lieblingsrippe getroffen!");
        put(Personality.MANIAC, Event.PHASE2,
                "Plan B! Ich hatte nie einen Plan A!",
                "Mein linkes Gehirn will aufgeben, mein rechtes will DICH!");
        put(Personality.MANIAC, Event.PHASE3,
                "Jetzt wird's lustig! Ich zähle bis drei! Eins, VIER!",
                "Ich drück einfach alle Knöpfe gleichzeitig!");
        put(Personality.MANIAC, Event.KILL,
                "Ups! Kaputt gemacht! Holst du einen neuen {p}?", "Hahahaha! Nochmal! Komm wieder, {p}!");
        put(Personality.MANIAC, Event.BEG,
                "Okay okay okay, Pause! Hast du schon mal Waffenstillstand probiert? Rechtsklick! Macht süchtig!");
        put(Personality.MANIAC, Event.SPARED,
                "Du lässt mich LEBEN?! Das ist das Verrückteste, was mir heute passiert ist! Und ich hab heute einen Stern gegessen!");
        put(Personality.MANIAC, Event.DEFEAT,
                "Das… war… der beste Tag… meines Lebens…", "Hihi… alles dreht sich… ach, das bin ja ich…");

        // ---------------- Feigling
        put(Personality.COWARD, Event.GREET,
                "Oh nein. Oh nein nein nein. Du bist {p}, oder? Die haben gesagt, du kommst nicht!",
                "B-bleib, wo du bist! Ich bin sehr gefährlich! Meine {m} sagen das auch!",
                "Ich wollte das {a} nur verkaufen! Ich mein: beschützen! Beschützen!");
        put(Personality.COWARD, Event.FIGHT,
                "Okay, dann eben Gewalt! Bitte nicht ins Gesicht!", "Hilfe! Ich meine: Angriff!");
        put(Personality.COWARD, Event.HURT,
                "Aua! Warum ich?!", "Das tut weh! Steht das in deiner Lizenz?!",
                "Ich sag's meiner Mutter! Die ist ein schwarzes Loch!");
        put(Personality.COWARD, Event.PHASE2,
                "{m}! Rettet mich! Es zahlt sich aus! Irgendwann!", "Ich hab Angst, also bin ich gefährlich!");
        put(Personality.COWARD, Event.PHASE3,
                "Ich will nach Hause! Ich HABE kein Zuhause!", "Wenn ich schon untergehe, dann laut!");
        put(Personality.COWARD, Event.KILL,
                "I-ich hab gewonnen? Ich hab GEWONNEN! Mama!", "Sorry! Sorry! Das war ein Reflex!");
        put(Personality.COWARD, Event.BEG,
                "Bitte, bitte, bitte! Ich gebe dir alles! Lass mich einfach gehen – Rechtsklick, nur ein Klick!",
                "Gnade! Ich hab Familie! Okay, ich hab einen Fisch! Aber der verlässt sich auf mich! Rechtsklick, bitte!");
        put(Personality.COWARD, Event.SPARED,
                "Danke! Danke! Ich ändere mein Leben! Ich werde Gärtner! In einem anderen Universum!");
        put(Personality.COWARD, Event.DEFEAT,
                "Ich… wusste es… heute ist nicht… mein Tag…", "Sagt meinem Fisch… dass ich…");

        // ---------------- Rächer
        put(Personality.AVENGER, Event.GREET,
                "{p}. Du erinnerst dich nicht an mich. Natürlich nicht. Ein Rift wie deins hat {h} zerrissen.",
                "Jedes Portal, das du öffnest, reißt irgendwo eine Welt auf. Meine war {h}.",
                "Ich habe auf dich gewartet. Drei Zeitlinien lang.");
        put(Personality.AVENGER, Event.FIGHT,
                "Für {h}!", "Das hier ist nicht persönlich. Doch. Es ist zutiefst persönlich.");
        put(Personality.AVENGER, Event.HURT,
                "Mehr Schmerz, als ich schon kenne, gibt es nicht.",
                "Triff mich ruhig. Ich spüre seit {h} nichts mehr.");
        put(Personality.AVENGER, Event.PHASE2,
                "Sie haben mich gewarnt, dass du stark bist. Sie sind alle fort.",
                "Das {a} wird meine Welt zurückbringen. Und du stehst im Weg.");
        put(Personality.AVENGER, Event.PHASE3,
                "Wenn ich falle, war alles umsonst. Also falle ich nicht!",
                "Ich sehe ihre Gesichter. Jedes Mal, wenn du zuschlägst.");
        put(Personality.AVENGER, Event.KILL,
                "Eins zu… unendlich. Das reicht nicht.", "Ruh dich aus, {p}. Ich kann das nicht mehr.");
        put(Personality.AVENGER, Event.BEG,
                "Wenn du mich gehen lässt – ein Rechtsklick –, versuche ich, {h} anders zu retten. Ohne Rache.");
        put(Personality.AVENGER, Event.SPARED,
                "Du lässt mich gehen? Dann ist vielleicht noch etwas von dir übrig, das man nicht hassen muss.");
        put(Personality.AVENGER, Event.DEFEAT,
                "{h}… ich komme… nach Hause…", "Es tut mir… leid… für euch alle…");

        // ---------------- Händler
        put(Personality.SALESMAN, Event.GREET,
                "Willkommen, willkommen, {p}! Darf ich dir das {a} anbieten? Nur heute: ein Leben im Tausch!",
                "Ah, ein Kunde! Kein Geld? Kein Problem, ich nehme auch Seelen.",
                "Kennst du schon unser Rift-Abo? Einmal zahlen, unendlich viele Universen verlieren!");
        put(Personality.SALESMAN, Event.FIGHT,
                "Reklamation? Dafür bin ich nicht zuständig! Doch, bin ich – mit Gewalt!",
                "Kaufen oder untergehen, {p}. Wir haben auch Ratenzahlung.");
        put(Personality.SALESMAN, Event.HURT,
                "Das gibt einen Kratzer im Lack! Das zahlst du!", "Hey, das ist Ausstellungsware!", "Rabatt verweigert!");
        put(Personality.SALESMAN, Event.PHASE2,
                "Neues Angebot: Du verlierst, ich behalte alles!", "Ich hole meine Premium-{m}! Die kosten extra!");
        put(Personality.SALESMAN, Event.PHASE3,
                "Räumungsverkauf! Alles muss raus – vor allem du!",
                "Letzte Chance: Garantieverlängerung für dein Leben!");
        put(Personality.SALESMAN, Event.KILL,
                "Danke für deinen Einkauf, {p}! Beehre uns bald wieder!",
                "Und wieder ein zufriedener Kunde. Glaube ich. Er sagt nichts mehr.");
        put(Personality.SALESMAN, Event.BEG,
                "Okay, okay! Deal! Du lässt mich gehen, ich gebe dir einen Sonderrabatt! Ein Rechtsklick – und der Vertrag steht!",
                "Lass uns verhandeln! Kartuschen, Fluid, alles! Rechtsklick, und es gehört dir!");
        put(Personality.SALESMAN, Event.SPARED,
                "Ein Geschäft auf Augenhöhe! Hier, deine Ware. Und eine Kundenkarte – du sammelst jetzt Punkte!");
        put(Personality.SALESMAN, Event.DEFEAT,
                "Geschäft… geschlossen…", "Keine… Rückgabe… möglich…");
    }

    private static final Map<BossAbility, String[]> ABILITY_LINES = new EnumMap<>(BossAbility.class);

    static {
        ABILITY_LINES.put(BossAbility.GRAVITY_PULSE, new String[]{"Spür die Gravitation!", "Komm her, {p}!",
                "Bei {g} ziehe ich dich, wohin ich will!"});
        ABILITY_LINES.put(BossAbility.PLASMA_BEAM, new String[]{"Plasma! Frisch aus dem Sternenkern!", "Halt still fürs Foto!",
                "Das {a} lädt auf!"});
        ABILITY_LINES.put(BossAbility.SUMMON_MINIONS, new String[]{"{m}! An die Arbeit!", "Ich habe Freunde. Bezahlte Freunde!"});
        ABILITY_LINES.put(BossAbility.BLINK, new String[]{"Hinter dir!", "Raum ist nur ein Vorschlag."});
        ABILITY_LINES.put(BossAbility.TIME_WARP, new String[]{"Die Zeit gehört mir!", "Du wirst langsam, {p}. Das war ich."});
        ABILITY_LINES.put(BossAbility.GROUND_SLAM, new String[]{"BODENPROBE!", "Achtung, Landung!"});
    }

    // ================================================================== Plot erzeugen

    public static BossPlot createPlot(RandomSource r, @Nullable UniverseProfile u) {
        BossPlot plot = new BossPlot();
        plot.name = NameGen.alienName(r);
        plot.artifact = NameGen.artifact(r);
        plot.minions = NameGen.minions(r);
        plot.homeworld = NameGen.universeName(r);
        plot.sender = NameGen.sender(r);
        plot.catchphrase = NameGen.alienName(r) + "-" + NameGen.alienName(r).toLowerCase(Locale.ROOT) + "!";
        plot.personality = Personality.values()[r.nextInt(Personality.values().length)];
        plot.twist = r.nextFloat() < 0.3f ? Twist.NONE : Twist.values()[1 + r.nextInt(Twist.values().length - 1)];
        plot.willBeg = r.nextFloat() < plot.personality.begChance;
        plot.skin = r.nextInt(4);

        // Titel und Motiv mit Platzhaltern für Universum/Artefakt
        Ctx neutral = new Ctx("Reisender", 1f, 1f, u, 0, "", 0);
        plot.title = fill(NameGen.pick(r, TITLES), plot, neutral);
        plot.motive = NameGen.pick(r, MOTIVES);

        // Fähigkeiten passend zur Physik des Universums gewichten
        List<BossAbility> pool = new ArrayList<>(List.of(BossAbility.values()));
        if (u != null) {
            if (u.gravity < 0.8) { pool.add(BossAbility.GRAVITY_PULSE); pool.add(BossAbility.GROUND_SLAM); }
            if (u.gravity > 1.25) pool.add(BossAbility.GROUND_SLAM);
            if (u.temperature == UniverseProfile.Temperature.HOT) pool.add(BossAbility.PLASMA_BEAM);
            if (u.temperature == UniverseProfile.Temperature.COLD) pool.add(BossAbility.TIME_WARP);
        }
        if (plot.personality == Personality.BUREAUCRAT || plot.personality == Personality.COWARD) pool.add(BossAbility.SUMMON_MINIONS);
        if (plot.personality == Personality.MANIAC) pool.add(BossAbility.BLINK);
        plot.abilities = new ArrayList<>();
        while (plot.abilities.size() < 2) {
            BossAbility a = pool.get(r.nextInt(pool.size()));
            if (!plot.abilities.contains(a)) plot.abilities.add(a);
        }
        if (u != null && u.danger >= 4) {
            for (int tries = 0; tries < 10 && plot.abilities.size() < 3; tries++) {
                BossAbility a = pool.get(r.nextInt(pool.size()));
                if (!plot.abilities.contains(a)) plot.abilities.add(a);
            }
        }
        return plot;
    }

    // ================================================================== Dialoge

    public static String line(BossPlot plot, Event event, Ctx ctx, RandomSource r) {
        Map<Event, String[]> byEvent = LINES.get(plot.personality);
        String[] pool = byEvent != null ? byEvent.get(event) : null;
        if (pool == null || pool.length == 0) pool = GENERIC.get(event);
        StringBuilder sb = new StringBuilder(NameGen.pick(r, pool));

        boolean dying = event == Event.DEFEAT || event == Event.DEFEAT_BEGGING;
        if (!dying) {
            if (event == Event.GREET && ctx.deaths() > 0) {
                sb.append(' ').append(NameGen.pick(r, RETURNING));
            } else if ((event == Event.HURT || event == Event.FIGHT || event == Event.IDLE)
                    && ctx.playerHealth() < 0.3f && r.nextFloat() < 0.6f) {
                sb.append(' ').append(NameGen.pick(r, PLAYER_LOW));
            } else if (ctx.universe() != null && r.nextFloat() < 0.35f && event != Event.BEG && event != Event.SPARED) {
                sb.append(' ').append(physicsRemark(ctx.universe(), r));
            } else if (event == Event.HURT && !ctx.heldItem().isEmpty() && r.nextFloat() < 0.25f) {
                sb.append(' ').append(NameGen.pick(r, ITEM_REMARKS));
            } else if (ctx.fightSeconds() > 120 && r.nextFloat() < 0.3f) {
                sb.append(' ').append(NameGen.pick(r, LONG_FIGHT));
            }
            if (r.nextFloat() < 0.08f && event != Event.BEG) sb.append(' ').append(plot.catchphrase);
        }
        return fill(sb.toString(), plot, ctx);
    }

    public static String abilityLine(BossPlot plot, BossAbility ability, Ctx ctx, RandomSource r) {
        return fill(NameGen.pick(r, ABILITY_LINES.get(ability)), plot, ctx);
    }

    /** Bemerkung über die Naturgesetze des aktuellen Universums. */
    private static String physicsRemark(UniverseProfile u, RandomSource r) {
        List<String> options = new ArrayList<>();
        if (u.gravity < 0.75) {
            options.add("Bei {g} schwebst du ja wie ein Staubkorn.");
            options.add("In dieser lächerlichen Schwerkraft fällt sogar dein Plan langsamer.");
            options.add("Spring nur, {p} – bei {g} landest du erst morgen.");
        } else if (u.gravity > 1.25) {
            options.add("Spürst du die {g}? Jeder Schritt ein Kniefall vor mir.");
            options.add("Hier wiegt sogar deine Hoffnung mehr.");
            options.add("Fall lieber nicht hin, {p}. Bei {g} tut das richtig weh.");
        }
        switch (u.temperature) {
            case COLD -> {
                options.add("Deine Finger zittern. Leder hätte geholfen.");
                options.add("Kalt hier, nicht wahr? Ich habe die Heizung abbestellt.");
            }
            case HOT -> {
                options.add("Schwitzt du? Das ist die Angst. Oder die Hitze.");
                options.add("Hier kocht sogar das Wasser vor Wut. Wobei – hier gibt's nur Lava.");
            }
            default -> { }
        }
        switch (u.atmosphere) {
            case TOXIC -> options.add("Atme ruhig tief ein. Ach nein, lieber nicht.");
            default -> { }
        }
        if (options.isEmpty()) {
            options.add("Normale Physik. Wie langweilig. Wie du.");
            options.add("Selbst die Naturgesetze hier sind auf meiner Seite.");
        }
        return options.get(r.nextInt(options.size()));
    }

    public static String twistReveal(BossPlot plot, Ctx ctx) {
        String s = switch (plot.twist) {
            case ALTERNATE_SELF -> "Erkennst du mich nicht, {p}? Ich BIN du. Aus einer Zeitlinie, in der du verloren hast.";
            case PUPPET -> "Glaubst du, ich mache das freiwillig? Der Rat der Konstanten hält meine Fäden!";
            case CLONE -> "Du glaubst, du kämpfst gegen EINEN von mir?";
            case VICTIM -> "Weißt du, was deine Rifts anrichten? Jedes Portal reißt eine Wunde in {u}!";
            case PHYSICS -> "Ich habe die Gravitationskonstante umgeschrieben! Schau nach oben, {p}!";
            default -> "";
        };
        return fill(s, plot, ctx);
    }

    // ================================================================== Auftrag (Hook)

    public static List<Component> hookMessages(BossPlot plot, UniverseProfile u, String player, int distance, String direction, RandomSource r) {
        Ctx ctx = new Ctx(player, 1f, 1f, u, 0, "", 0);
        List<Component> out = new ArrayList<>();
        out.add(Component.literal("[Rift-Funk] ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                .append(Component.literal("Eingehende Übertragung aus " + u.title() + " „" + u.name + "“ … Absender: "
                        + plot.sender + ".").withStyle(ChatFormatting.GRAY)));

        StringBuilder story = new StringBuilder();
        story.append(NameGen.pick(r, HOOK_OPENERS)).append(' ');
        story.append("{b} {t} ").append(plot.motive).append(". ");
        story.append(stakes(u, r)).append(' ');
        story.append(NameGen.pick(r, HOOK_PLEAS));
        out.add(Component.literal("„" + fill(story.toString(), plot, ctx) + "“").withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));

        out.add(Component.literal("➜ Ziel: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(plot.fullName()).withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(" – ca. " + distance + " Blöcke " + direction + ". Folge dem Lichtstrahl.")
                        .withStyle(ChatFormatting.YELLOW)));
        return out;
    }

    private static String stakes(UniverseProfile u, RandomSource r) {
        List<String> options = new ArrayList<>();
        if (u.gravity < 0.75) options.add("Bei nur {g} hält uns ohnehin kaum etwas am Boden – noch ein Stoß, und alles fliegt davon.");
        if (u.gravity > 1.3) options.add("Bei {g} können wir nicht mal fliehen. Wir kriechen nur.");
        if (u.atmosphere == UniverseProfile.Atmosphere.TOXIC) options.add("Unsere Atemfilter halten nicht mehr lange.");
        if (u.temperature == UniverseProfile.Temperature.COLD) options.add("Die Kälte frisst sich in unsere Häuser.");
        if (u.temperature == UniverseProfile.Temperature.HOT) options.add("Die Lavaseen steigen. Jeden Tag ein Stück.");
        options.add("Uns läuft die Zeit davon.");
        options.add("Die Hälfte von uns ist schon in ein Paralleluniversum geflohen. Die andere Hälfte ist die falsche Hälfte.");
        return options.get(r.nextInt(options.size()));
    }

    // ================================================================== Epilog

    public static String epithet(Outcome outcome, UniverseProfile u, RandomSource r) {
        String[] options = switch (outcome) {
            case SPARED -> new String[]{"die Gnädige Hand", "Friedensrift", "Herz des Multiversums", "Bewahrer der Zweiten Chance"};
            case EXECUTED -> new String[]{"die Kalte Klinge", "Gnadenlos von " + u.id.code(), "das Ende aller Bitten"};
            default -> new String[]{"Rift-Bezwinger", "Schrecken von " + u.id.code(), "der Grüne Blitz",
                    "Retter von " + u.name, "Gravitationsbrecher"};
        };
        return NameGen.pick(r, options);
    }

    public static List<String> epilogue(BossPlot plot, Outcome outcome, @Nullable UniverseProfile u, String player, String epithet) {
        Ctx ctx = new Ctx(player, 1f, 0f, u, 0, "", 0);
        List<String> lines = new ArrayList<>();
        String where = u != null ? u.name : "diese Realität";
        switch (outcome) {
            case SPARED -> {
                lines.add("{b} verschwindet durch ein violettes Rift. Ob {b} sich wirklich ändert? Das Multiversum wird es zeigen.");
                lines.add("In " + where + " erzählt man sich jetzt eine Geschichte über Gnade. Dein Name dort: „" + epithet + "“.");
            }
            case EXECUTED -> {
                lines.add("{b} hatte um Gnade gebeten. Die Bewohner von " + where + " sind dir dankbar – und ein bisschen ängstlich.");
                lines.add("Sie nennen dich „" + epithet + "“.");
            }
            default -> {
                lines.add("{b} {t} ist gefallen. " + where + " atmet auf.");
                lines.add("Die Bewohner nennen dich ab heute „" + epithet + "“.");
            }
        }
        String twist = switch (plot.twist) {
            case ALTERNATE_SELF -> "Im Staub liegt ein Ausweis – mit deinem Namen, aber einem fremden Gesicht.";
            case PUPPET -> "Irgendwo notiert der Rat der Konstanten deinen Namen. {b} wird nicht der letzte Bote gewesen sein.";
            case CLONE -> "Einer der Klone ist entkommen. Vielleicht. Wer weiß das bei Klonen schon.";
            case VICTIM -> "Doch wo dein Rift aufging, flimmert die Luft noch lange. Vielleicht hatte {b} nicht in allem unrecht.";
            case PHYSICS -> "Die Gravitation pendelt sich wieder ein. Die Vögel fliegen wieder richtig herum.";
            default -> "";
        };
        if (!twist.isEmpty()) lines.add(twist);
        lines.replaceAll(s -> fill(s, plot, ctx));
        return lines;
    }

    /** Ein geschriebenes Buch, das die ganze Geschichte festhält. */
    public static ItemStack chronicle(BossPlot plot, Outcome outcome, @Nullable UniverseProfile u, List<String> log, List<String> epilogue) {
        List<String> pages = new ArrayList<>();
        if (u != null) {
            pages.add(u.name + "\n" + u.title() + "\n\nGravitation: " + u.gravityText() + "\nAtmosphäre: "
                    + u.atmosphere.label.split(" ")[0] + "\nTemperatur: " + u.temperature.label.split(" ")[0]
                    + "\nGelände: " + u.terrain.label);
        }
        pages.add("Der Ruf\n\n" + capitalize(plot.sender) + " bat um Hilfe:\n" + plot.fullName() + " "
                + fill(plot.motive, plot, new Ctx("du", 1f, 1f, u, 0, "", 0)) + ".");
        StringBuilder page = new StringBuilder("Gesprochene Worte\n\n");
        for (String l : log) {
            String entry = "„" + l + "“\n\n";
            if (page.length() + entry.length() > 230) {
                pages.add(page.toString());
                page = new StringBuilder();
            }
            page.append(entry);
        }
        if (page.length() > 0) pages.add(page.toString());
        String result = switch (outcome) {
            case SPARED -> "Ausgang: verschont";
            case EXECUTED -> "Ausgang: hingerichtet";
            default -> "Ausgang: besiegt";
        };
        pages.add(result + "\n\n" + String.join("\n\n", epilogue));

        List<Filterable<Component>> bookPages = new ArrayList<>();
        for (String p : pages) bookPages.add(Filterable.passThrough(Component.literal(p)));
        String title = "Chronik " + (u != null ? u.id.code() : plot.name);
        if (title.length() > 32) title = title.substring(0, 32);

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough(title), "Rift-Archiv", 0, bookPages, true));
        return book;
    }

    // ================================================================== Platzhalter

    public static String fill(String template, BossPlot plot, Ctx ctx) {
        UniverseProfile u = ctx.universe();
        return template
                .replace("{p}", ctx.player())
                .replace("{b}", plot.name)
                .replace("{t}", plot.title)
                .replace("{a}", plot.artifact)
                .replace("{m}", plot.minions)
                .replace("{h}", plot.homeworld)
                .replace("{u}", u != null ? u.name : "dieser Realität")
                .replace("{c}", u != null ? u.id.code() : "?")
                .replace("{g}", u != null ? String.format(Locale.GERMAN, "%.2f g", u.gravity) : "1,00 g")
                .replace("{n1}", String.valueOf(ctx.deaths() + 1))
                .replace("{n}", String.valueOf(ctx.deaths()))
                .replace("{item}", ctx.heldItem())
                .replace("{sec}", String.valueOf(ctx.fightSeconds()))
                .replace("{catch}", plot.catchphrase);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private StoryEngine() {}
}
