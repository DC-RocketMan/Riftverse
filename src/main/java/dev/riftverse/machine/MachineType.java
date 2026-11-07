package dev.riftverse.machine;

/** Die drei Maschinen der Herstellungskette für die Rift-Kanone. */
public enum MachineType {
    /** Raw Quantum + Redstone → Quanten-Barren */
    REFINER("quantum_refiner", 0x39FF14),
    /** Quanten-Barren + Glas/Schleim → Portal-Fluid-Kartuschen */
    SYNTHESIZER("fluid_synthesizer", 0x2EE8B5),
    /** Rift-Kern, Kanonen-Rahmen, Mk-II-Upgrade */
    ASSEMBLER("rift_assembler", 0xB565FF);

    private final String id;
    private final int color;

    MachineType(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    /** Akzentfarbe für die GUI. */
    public int color() {
        return color;
    }

    public String translationKey() {
        return "block.riftverse." + id;
    }
}
