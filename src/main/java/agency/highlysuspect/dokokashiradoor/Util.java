package agency.highlysuspect.dokokashiradoor;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkManager;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class Util {
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
	
	/**
	 * Check that this position, as well as its four neighboring blocks, are loaded
	 */
	public static boolean isPositionAndNeighborsLoaded(ChunkManager cm, BlockPos pos) {
		return allLoaded(cm, pos, pos.offset(Direction.NORTH), pos.offset(Direction.EAST), pos.offset(Direction.SOUTH), pos.offset(Direction.WEST));
	}
	
	public static boolean allLoaded(ChunkManager cm, BlockPos... positions) {
		for(BlockPos p : positions) {
			if(!cm.isChunkLoaded(p.getX() / 16, p.getZ() / 16)) return false;
		}
		return true;
	}
	
	public static class RandomSelectableMap<K, V> extends Object2ObjectOpenHashMap<K, V> {
		public RandomSelectableMap() {
			super();
		}
		
		public RandomSelectableMap(Map<? extends K, ? extends V> m) {
			super(m);
		}
		
		public RandomSelectableMap(Collection<V> values, Function<V, K> keyExtractor) {
			super();
			for(V v : values) {
				put(keyExtractor.apply(v), v);
			}
		}
		
		public @Nullable K randomKey(Random random) {
			return key[random.nextInt(key.length)];
		}
		
		public @Nullable V randomValue(Random random) {
			return value[random.nextInt(value.length)];
		}
		
		public V randomRemoveIf(Random random, BiPredicate<K, V> pred) {
			if(isEmpty()) return null;
			
			K rand = randomKey(random);
			if(rand == null) return null;
			V v = get(rand);
			
			if(pred.test(rand, v)) {
				remove(rand);
				return v;
			} else return null;
		}
		
		public List<V> asList() {
			//Convenience
			return values().stream().toList();
		}
	}
	
	public static class RandomSelectableSet<T> extends ObjectOpenHashSet<T> {
		public RandomSelectableSet() {
			super();
		}
		
		public RandomSelectableSet(Collection<T> t) {
			super(t);
		}
		
		public @Nullable T randomKey(Random random) {
			return key[random.nextInt(key.length)];
		}
		
		public @Nullable T randomRemoveIf(Random random, Predicate<T> pred) {
			if(isEmpty()) return null;
			
			T rand = randomKey(random);
			if(rand == null) return null;
			
			if(pred.test(rand)) {
				remove(rand);
				return rand;
			} else return null;
		}
		
		public List<T> asList() {
			return stream().toList();
		}
	}
}
