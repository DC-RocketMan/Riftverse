package dev.riftverse.tech;

import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Chrono-Speicher (T3): 256 Mio. FE, Stufe III. Gibt gespeicherte Energie 4× schneller ab, als er sie aufnimmt:
 * laden höchstens 2.048 FE/t, abgeben ein volles Stufe-III-Paket (8.192 FE/t).
 * <p>
 * Verbund: Chrono-Speicher, die sich berühren, gleichen ihre Ladung jeden Tick aus und wirken wie ein großer Speicher
 * (bis 9 × 9 × 9). Jeder Speicher im Verbund zählt mit, auch beim Lade- und Abgabetempo, und jeder kann am Netz hängen.
 */
public class ChronoSpeicherBlockEntity extends XenitZelleBlockEntity {
    public static final int CHRONO_CAPACITY = 256_000_000;
    public static final VoltageTier CHRONO_TIER = VoltageTier.III;
    /** Laden pro Tick; abgegeben wird viermal so schnell. */
    public static final int CHARGE_RATE = 2_048;
    public static final int DISCHARGE_RATE = CHARGE_RATE * 4;
    /** So viel FE schiebt ein Speicher pro Tick höchstens zu einem Nachbarn im Verbund. */
    public static final int SHARE_RATE = 1_000_000;

    private int chargedThisTick;

    public ChronoSpeicherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHRONO_SPEICHER.get(), pos, state, CHRONO_CAPACITY, CHRONO_TIER);
    }

    @Override
    public void serverTick() {
        chargedThisTick = 0;
        super.serverTick();
        if (level == null) return;
        // Verbund: mit den Nachbarn in Richtung +x, +y, +z ausgleichen (die anderen Richtungen übernimmt der Nachbar)
        for (Direction d : new Direction[]{Direction.EAST, Direction.UP, Direction.SOUTH}) {
            BlockPos n = worldPosition.relative(d);
            if (!level.isLoaded(n) || !(level.getBlockEntity(n) instanceof ChronoSpeicherBlockEntity other)) continue;
            int diff = energy.stored() - other.energy().stored();
            int move = Math.max(-SHARE_RATE, Math.min(SHARE_RATE, diff / 2));
            if (move > 0) other.energy().add(energy.take(move));
            else if (move < 0) energy.add(other.energy().take(-move));
        }
    }

    /** Anzahl der Speicher, die über Nachbarschaft zusammenhängen (für die Anzeige, höchstens 729). */
    public int verbund() {
        if (level == null) return 1;
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        seen.add(worldPosition);
        queue.add(worldPosition);
        while (!queue.isEmpty() && seen.size() < 729) {
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (Math.abs(n.getX() - worldPosition.getX()) > 8 || Math.abs(n.getY() - worldPosition.getY()) > 8
                        || Math.abs(n.getZ() - worldPosition.getZ()) > 8) continue;
                if (!seen.contains(n) && level.isLoaded(n) && level.getBlockEntity(n) instanceof ChronoSpeicherBlockEntity) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        return seen.size();
    }

    // ------------------------------------------------------------------ Netz: 2.048 FE/t rein, 8.192 FE/t raus

    @Override
    public int getOfferedEnergy() {
        return Math.min(energy.stored(), DISCHARGE_RATE);
    }

    @Override
    public int getDemandedEnergy() {
        return Math.max(0, Math.min(energy.space(), CHARGE_RATE - chargedThisTick));
    }

    @Override
    public int injectEnergy(int amount, VoltageTier tier) {
        int accepted = energy.add(Math.min(amount, Math.max(0, CHARGE_RATE - chargedThisTick)));
        chargedThisTick += accepted;
        return accepted;
    }
}
