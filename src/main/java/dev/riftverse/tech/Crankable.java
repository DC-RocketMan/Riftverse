package dev.riftverse.tech;

import net.minecraft.world.entity.player.Player;

/** Lässt sich mit der Handkurbel antreiben (Garagenpresse, Kurbel-Dynamo). */
public interface Crankable {
    /** Wird jeden Tick aufgerufen, solange ein Spieler mit der Handkurbel kurbelt. */
    void crank(Player player);
}
