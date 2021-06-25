package agency.highlysuspect.dokokashiradoor.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DoorUtil {
	public static void sneakyOpenDoor(World world, BlockPos doorTop, BlockState topState) {
		BlockState topOpen = topState.with(DoorBlock.OPEN, true);
		BlockState bottomOpen = topOpen.cycle(DoorBlock.HALF);
		
		sneakySetBlockstate(world, doorTop, topOpen);
		sneakySetBlockstate(world, doorTop.down(), bottomOpen);
	}
	
	public static void loudlyOpenDoor(Entity openerForGameEvents, World world, BlockPos doorTop, BlockState topState) {
		DoorBlock db = (DoorBlock) topState.getBlock();
		db.setOpen(openerForGameEvents, world, topState, doorTop, true);
	}
	
	public static void sneakySwapHinge(World world, BlockPos doorTop, BlockState topState) {
		BlockState topStateSwapped = topState.cycle(DoorBlock.HINGE);
		BlockState bottomStateSwapped = topStateSwapped.cycle(DoorBlock.HALF);
		
		sneakySetBlockstate(world, doorTop, topStateSwapped);
		sneakySetBlockstate(world, doorTop.down(), bottomStateSwapped);
	}
	
	public static void sneakySetBlockstate(World world, BlockPos pos, BlockState state) {
		world.setBlockState(pos, state, 0);
	}
}
