package agency.highlysuspect.dokokashiradoor.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.event.GameEvent;

public class DoorUtil {
	public static void sneakyOpenDoor(World world, BlockPos doorTop, BlockState topState) {
		BlockState topOpen = topState.with(DoorBlock.OPEN, true);
		BlockState bottomOpen = topOpen.cycle(DoorBlock.HALF);
		
		sneakySetBlockstate(world, doorTop, topOpen);
		sneakySetBlockstate(world, doorTop.down(), bottomOpen);
	}
	
	public static void silentlyOpenDoor(Entity openerForGameEvents, World world, BlockPos doorTop, BlockState topState) {
		world.setBlockState(doorTop, topState.with(DoorBlock.OPEN, true), Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD);
		//this.playOpenCloseSound(world, pos, open);
		world.emitGameEvent(openerForGameEvents, GameEvent.BLOCK_OPEN, doorTop);
		//db.setOpen(openerForGameEvents, world, topState, doorTop, true);
	}
	
	public static void playOpenSound(World world, BlockPos pos) {
		world.syncWorldEvent(null, WorldEvents.WOODEN_DOOR_OPENS, pos, 0);
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
