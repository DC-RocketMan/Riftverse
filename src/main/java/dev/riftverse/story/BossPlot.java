package dev.riftverse.story;

import dev.riftverse.boss.BossAbility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/** Die generierte Handlung rund um einen Boss. Wird am Boss gespeichert. */
public class BossPlot {
    public String name = "Glorbax";
    public String title = "der Unbekannte";
    public String artifact = "Paradox-Ei";
    public String minions = "Handlanger";
    public String homeworld = "Nirgendwo";
    public String sender = "eine anonyme Stimme";
    public String catchphrase = "Zorp-zorp!";
    public String motive = "will das Multiversum neu sortieren";
    public Personality personality = Personality.MEGALOMANIAC;
    public Twist twist = Twist.NONE;
    public List<BossAbility> abilities = new ArrayList<>(List.of(BossAbility.PLASMA_BEAM, BossAbility.GROUND_SLAM));
    public boolean willBeg;
    public int skin;

    public String fullName() {
        return name + " " + title;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Name", name);
        t.putString("Title", title);
        t.putString("Artifact", artifact);
        t.putString("Minions", minions);
        t.putString("Homeworld", homeworld);
        t.putString("Sender", sender);
        t.putString("Catch", catchphrase);
        t.putString("Motive", motive);
        t.putString("Personality", personality.name());
        t.putString("Twist", twist.name());
        t.putBoolean("WillBeg", willBeg);
        t.putInt("Skin", skin);
        ListTag list = new ListTag();
        for (BossAbility a : abilities) list.add(StringTag.valueOf(a.name()));
        t.put("Abilities", list);
        return t;
    }

    public static BossPlot load(CompoundTag t) {
        BossPlot p = new BossPlot();
        p.name = t.getString("Name");
        p.title = t.getString("Title");
        p.artifact = t.getString("Artifact");
        p.minions = t.getString("Minions");
        p.homeworld = t.getString("Homeworld");
        p.sender = t.getString("Sender");
        p.catchphrase = t.getString("Catch");
        p.motive = t.getString("Motive");
        p.personality = safeEnum(Personality.class, t.getString("Personality"), Personality.MEGALOMANIAC);
        p.twist = safeEnum(Twist.class, t.getString("Twist"), Twist.NONE);
        p.willBeg = t.getBoolean("WillBeg");
        p.skin = t.getInt("Skin");
        p.abilities = new ArrayList<>();
        ListTag list = t.getList("Abilities", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            BossAbility a = safeEnum(BossAbility.class, list.getString(i), null);
            if (a != null) p.abilities.add(a);
        }
        if (p.abilities.isEmpty()) p.abilities.add(BossAbility.PLASMA_BEAM);
        return p;
    }

    private static <E extends Enum<E>> E safeEnum(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
