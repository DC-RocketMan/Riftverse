package dev.riftverse.tech;

import com.mojang.serialization.MapCodec;
import dev.riftverse.registry.ModBlockEntities;
import net.minecraft.world.level.block.BaseEntityBlock;

/** Verarbeitende Maschine (Erzmühle, Walzbank, Legierungsofen, Garagenpresse). */
public class ProcessingBlock extends TechBlock {
    private final MachineKind kind;
    private final MapCodec<ProcessingBlock> codec;

    public ProcessingBlock(Properties properties, MachineKind kind) {
        super(properties, ModBlockEntities.PROCESSING, kind.electric());
        this.kind = kind;
        this.codec = simpleCodec(p -> new ProcessingBlock(p, kind));
    }

    public MachineKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }
}
