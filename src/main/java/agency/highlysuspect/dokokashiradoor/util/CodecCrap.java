package agency.highlysuspect.dokokashiradoor.util;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;

public class CodecCrap {
	public static <T> NbtElement writeNbt(Codec<T> codec, T thing) {
		return codec.encodeStart(NbtOps.INSTANCE, thing).getOrThrow();
	}
	
	public static <T> T readNbtAllowPartial(Codec<T> codec, NbtElement nbt) {
		return codec.parse(NbtOps.INSTANCE, nbt).getPartialOrThrow();
	}
	
	public static <T> Codec<ObjectOpenHashSet<T>> objectOpenHashSetCodec(Codec<T> elementCodec) {
		//probably slow as shit lol
		return elementCodec.listOf().xmap(ObjectOpenHashSet::new, s -> s.stream().toList());
	}
}
