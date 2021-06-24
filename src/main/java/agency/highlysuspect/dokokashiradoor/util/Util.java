package agency.highlysuspect.dokokashiradoor.util;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkManager;

import java.util.zip.CRC32;

public class Util {
	public static boolean isPositionAndNeighborsLoaded(ChunkManager cm, BlockPos pos) {
		return allLoaded(cm, pos, pos.north(), pos.east(), pos.south(), pos.west());
	}
	
	public static boolean allLoaded(ChunkManager cm, BlockPos... positions) {
		for(BlockPos p : positions) {
			if(!cm.isChunkLoaded(p.getX() / 16, p.getZ() / 16)) return false;
		}
		return true;
	}
	
	public static int checksumIntList(IntList list) {
		CRC32 crc = new CRC32();
		
		for(int i = 0; i < list.size(); i++) {
			crc.update(list.getInt(i));
		}
		
		//Boo, hiss
		//I should just make the code actually operate on longs lol
		return (int) (crc.getValue() ^ (crc.getValue() >> 32));
	}
}
