package agency.highlysuspect.dokokashiradoor;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record Gateway(BlockPos doorTopPos, DoorBlock doorBlock, List<Block> frame, Direction facing) {
	//For serialization
	public record Proto(BlockPos doorTopPos, Identifier doorBlockId, List<Identifier> frameIds, Direction facing) {
		public static final Codec<Proto> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(Proto::doorTopPos),
			Identifier.CODEC.fieldOf("doorBlock").forGetter(Proto::doorBlockId),
			Identifier.CODEC.listOf().fieldOf("frame").forGetter(Proto::frameIds),
			Direction.CODEC.fieldOf("facing").forGetter(Proto::facing)
		).apply(i, Proto::new));
		
		public static Proto lift(Gateway gateway) {
			Preconditions.checkNotNull(gateway);
			
			return new Proto(
				gateway.doorTopPos,
				Registry.BLOCK.getId(gateway.doorBlock),
				gateway.frame.stream().map(Registry.BLOCK::getId).collect(Collectors.toList()),
				gateway.facing
			);
		}
		
		public DataResult<Gateway> validateAndDrop() {
			if(!Registry.BLOCK.containsId(doorBlockId)) return DataResult.error("No such block " + doorBlockId);
			if(!(Registry.BLOCK.get(doorBlockId) instanceof DoorBlock doorBlock)) return DataResult.error("Block " + doorBlockId + " is not instanceof DoorBlock");
			
			if(frameIds.size() != 7) return DataResult.error("Expected 7 frame blocks, found " + frameIds.size());
			List<Block> frameBlocks = new ArrayList<>();
			for(Identifier id : frameIds) {
				if(!Registry.BLOCK.containsId(id)) return DataResult.error("No such block " + id + " in frame");
				frameBlocks.add(Registry.BLOCK.get(id));
			}
			
			return DataResult.success(new Gateway(doorTopPos, doorBlock, frameBlocks, facing));
		}
	}
	
	public static final Codec<Gateway> CODEC = Proto.CODEC.comapFlatMap(Proto::validateAndDrop, Proto::lift);
	
	public boolean equalButDifferentPositions(Gateway other) {
		return (!doorTopPos.equals(other.doorTopPos)) &&
			doorBlock.equals(other.doorBlock) &&
			frame.equals(other.frame);
	}
	
	public @Nullable Gateway recreate(ServerWorld world) {
		return readFromWorld(world, doorTopPos);
	}
	
	public static @Nullable Gateway readFromWorld(ServerWorld world, BlockPos doorTopPosMut) {
		BlockPos doorTopPos = doorTopPosMut.toImmutable();
		
		BlockState doorTopState = world.getBlockState(doorTopPos);
		Block maybeDoorBlock = doorTopState.getBlock();
		if(!(maybeDoorBlock instanceof DoorBlock doorBlock)) return null;
		if(!(Init.OPAQUE_DOORS.contains(doorBlock))) return null;
		
		//"facing" -> the direction the player faces, when they place a door
		//The *opposite* of facing, is the block edge that the door rests on.
		//Kinda backwards.
		Direction forwards = doorTopState.get(DoorBlock.FACING).getOpposite();
		Direction left = forwards.rotateYClockwise();
		Direction right = left.getOpposite();
		
		List<BlockState> frameStates = new ArrayList<>();
		BlockPos.Mutable cursor = doorTopPos.mutableCopy();
		
		// 345 //
		// 2T6 // door top
		// 1B7 // door bottom
		cursor.move(Direction.DOWN);
		cursor.move(left);
		frameStates.add(world.getBlockState(cursor)); //1
		cursor.move(Direction.UP);
		frameStates.add(world.getBlockState(cursor)); //2
		cursor.move(Direction.UP);
		frameStates.add(world.getBlockState(cursor)); //3
		cursor.move(right);
		frameStates.add(world.getBlockState(cursor)); //4
		cursor.move(right);
		frameStates.add(world.getBlockState(cursor)); //5
		cursor.move(Direction.DOWN);
		frameStates.add(world.getBlockState(cursor)); //6
		cursor.move(Direction.DOWN);
		frameStates.add(world.getBlockState(cursor)); //7
		
		if(frameStates.stream().anyMatch(s -> s.isAir() || !s.isOpaque())) return null;
		List<Block> frameBlocks = frameStates.stream().map(BlockState::getBlock).collect(Collectors.toList());
		
		return new Gateway(doorTopPos, doorBlock, frameBlocks, forwards);
	}
	
	public void arrive(World world, Gateway leftFrom, ServerPlayerEntity player) {
		//Find the vector from (current door -> player position)
		Vec3d currentDifference = player.getPos().subtract(Vec3d.ofBottomCenter(leftFrom.doorTopPos));
		float yawAdd = 0;
		
		//Rotate that vector according to the difference in angle between the two doors
		if(facing != leftFrom.facing) {
			if(facing.rotateYClockwise() == leftFrom.facing) {
				currentDifference = new Vec3d(currentDifference.z, currentDifference.y, -currentDifference.x);
				yawAdd = 270;
			} else if(facing.getOpposite() == leftFrom.facing) {
				currentDifference = new Vec3d(-currentDifference.x, currentDifference.y, -currentDifference.z);
				yawAdd = 180;
			} else {
				currentDifference = new Vec3d(-currentDifference.z, currentDifference.y, currentDifference.x);
				yawAdd = 90;
			}
		}
		
		//Apply that vector to the current door position
		Vec3d destPos = currentDifference.add(Vec3d.ofBottomCenter(doorTopPos));
		
		//Send the player off
		player.networkHandler.requestTeleport(destPos.x, destPos.y, destPos.z, player.getYaw() + yawAdd, player.getPitch());
		
		//Sneakily update the hinge on the destination door to match the hinge of the source door
		// (makes it look better)
		BlockState leftFromDoorState = world.getBlockState(leftFrom.doorTopPos);
		DoorHinge hinge = leftFromDoorState.get(DoorBlock.HINGE);
		world.setBlockState(doorTopPos, world.getBlockState(doorTopPos).with(DoorBlock.HINGE, hinge));
		//world.setBlockState(doorTopPos.down(), world.getBlockState(doorTopPos.down()).with(DoorBlock.HINGE, hinge));
		
		//Open the destination door
		BlockState doorState = world.getBlockState(doorTopPos);
		doorBlock.setOpen(null, world, doorState, doorTopPos, true);
	}
}
