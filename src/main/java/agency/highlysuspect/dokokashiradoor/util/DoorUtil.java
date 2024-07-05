package agency.highlysuspect.dokokashiradoor.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.function.Function;

public class DoorUtil {
	public static final Codec<DoorBlock> DOOR_CODEC = Registries.BLOCK.getCodec().comapFlatMap(block -> {
		if (block instanceof DoorBlock door) {
			return DataResult.success(door);
		}

		return DataResult.error(() -> "Block " + block + " is not instanceof DoorBlock");
	}, Function.identity());

	public static final PacketCodec<RegistryByteBuf, DoorBlock> DOOR_PACKET_CODEC = PacketCodecs.registryValue(RegistryKeys.BLOCK).xmap(block -> {
		if (block instanceof DoorBlock door) {
			return door;
		}

		throw new IllegalArgumentException("Block " + block + " is not instanceof DoorBlock");
	}, Function.identity());

	public static void sneakyOpenDoor(World world, BlockPos doorTop, BlockState topState) {
		BlockState topOpen = topState.with(DoorBlock.OPEN, true);
		BlockState bottomOpen = topOpen.cycle(DoorBlock.HALF);
		
		sneakySetBlockstate(world, doorTop, topOpen);
		sneakySetBlockstate(world, doorTop.down(), bottomOpen);
	}
	
	public static void silentlyOpenDoor(Entity openerForGameEvents, World world, BlockPos doorTop, BlockState topState) {
		world.setBlockState(doorTop, topState.with(DoorBlock.OPEN, true), Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
		//this.playOpenCloseSound(world, pos, open);
		world.emitGameEvent(openerForGameEvents, GameEvent.BLOCK_OPEN, doorTop);
	}
	
	public static void sneakySwapHinge(World world, BlockPos doorTop, BlockState topState) {
		BlockState topStateSwapped = topState.cycle(DoorBlock.HINGE);
		BlockState bottomStateSwapped = topStateSwapped.cycle(DoorBlock.HALF);
		
		sneakySetBlockstate(world, doorTop, topStateSwapped);
		sneakySetBlockstate(world, doorTop.down(), bottomStateSwapped);
	}
	
	public static void sneakySetBlockstate(World world, BlockPos pos, BlockState state) {
		world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
	}

	public static SoundEvent getOpenSound(BlockState state) {
		if (state.getBlock() instanceof DoorBlock door) {
			return door.getBlockSetType().doorOpen();
		}

		return SoundEvents.BLOCK_WOODEN_DOOR_OPEN;
	}
}
