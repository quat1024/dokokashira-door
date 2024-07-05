package agency.highlysuspect.dokokashiradoor.util;

import com.mojang.serialization.Codec;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.PacketCodec;

import java.util.List;
import java.util.function.Function;

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

	public static <B, V> PacketCodec<B, List<V>> validateMinLength(PacketCodec<B, List<V>> codec, int minLength) {
		return codec.xmap(list -> {
			if (list.size() < minLength) {
				throw new DecoderException(list.size() + " elements is less than min size of: " + minLength);
			}

			return list;
		}, Function.identity());
	}
}
