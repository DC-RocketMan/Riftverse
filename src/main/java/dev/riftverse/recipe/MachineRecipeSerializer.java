package dev.riftverse.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riftverse.tech.MachineKind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Ein Serializer pro Maschine; das JSON-Format ist überall gleich. */
public class MachineRecipeSerializer implements RecipeSerializer<MachineRecipe> {
    private final MapCodec<MachineRecipe> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec;

    public MachineRecipeSerializer(MachineKind kind) {
        this.codec = RecordCodecBuilder.mapCodec(i -> i.group(
                CountedIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(MachineRecipe::ingredients),
                ItemStack.CODEC.fieldOf("result").forGetter(MachineRecipe::result),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", 200).forGetter(MachineRecipe::time),
                Codec.STRING.optionalFieldOf("mode", "").forGetter(MachineRecipe::mode)
        ).apply(i, (ingredients, result, time, mode) -> new MachineRecipe(kind, ingredients, result, time, mode)));

        this.streamCodec = StreamCodec.composite(
                CountedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::ingredients,
                ItemStack.STREAM_CODEC, MachineRecipe::result,
                ByteBufCodecs.VAR_INT, MachineRecipe::time,
                ByteBufCodecs.STRING_UTF8, MachineRecipe::mode,
                (ingredients, result, time, mode) -> new MachineRecipe(kind, ingredients, result, time, mode));
    }

    @Override
    public MapCodec<MachineRecipe> codec() {
        return codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec() {
        return streamCodec;
    }
}
