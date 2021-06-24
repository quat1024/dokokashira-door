package agency.highlysuspect.dokokashiradoor.util;

import agency.highlysuspect.dokokashiradoor.Init;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;

public class CodecCrap {
	public static <T> T readNbt(Codec<T> codec, NbtElement nbt) {
		return codec.parse(NbtOps.INSTANCE, nbt).getOrThrow(false, Init.LOGGER::error);
	}
	
	public static <T> NbtElement writeNbt(Codec<T> codec, T thing) {
		return codec.encodeStart(NbtOps.INSTANCE, thing).getOrThrow(false, Init.LOGGER::error);
	}
	
	public static <T> T readNbtAllowPartial(Codec<T> codec, NbtElement nbt) {
		return codec.parse(NbtOps.INSTANCE, nbt).getOrThrow(true, Init.LOGGER::error);
	}
	
	public static <T> NbtElement writeNbtAllowPartial(Codec<T> codec, T thing) {
		return codec.encodeStart(NbtOps.INSTANCE, thing).getOrThrow(true, Init.LOGGER::error);
	}
	
	public static <T> Codec<ObjectOpenHashSet<T>> objectOpenHashSetCodec(Codec<T> elementCodec) {
		//probably slow as shit lol
		return elementCodec.listOf().xmap(ObjectOpenHashSet::new, s -> s.stream().toList());
	}
}
