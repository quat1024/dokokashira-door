package agency.highlysuspect.dokokashiradoor.util;

import agency.highlysuspect.dokokashiradoor.Init;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkManager;

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
}
