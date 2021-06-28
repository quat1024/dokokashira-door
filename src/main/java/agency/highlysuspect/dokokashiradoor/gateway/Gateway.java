package agency.highlysuspect.dokokashiradoor.gateway;

import agency.highlysuspect.dokokashiradoor.Init;
import agency.highlysuspect.dokokashiradoor.util.DoorUtil;
import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record Gateway(BlockPos doorTopPos, DoorBlock doorBlock, List<Block> frame, Direction facing) implements Comparable<Gateway> {
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
	
	public boolean stillExistsInWorld(World world) {
		return this.equals(recreate(world));
	}
	
	public @Nullable Gateway recreate(World world) {
		return readFromWorld(world, doorTopPos);
	}
	
	public static @Nullable Gateway readFromWorld(World world, BlockPos doorTopPosMut) {
		BlockPos doorTopPos = doorTopPosMut.toImmutable();
		
		BlockState doorTopState = world.getBlockState(doorTopPos);
		Block maybeDoorBlock = doorTopState.getBlock();
		if(!(maybeDoorBlock instanceof DoorBlock doorBlock)) return null;
		if(!Init.OPAQUE_DOORS.contains(doorBlock)) return null;
		
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
	
	public void arrive(World world, Gateway departureGateway, PlayerEntity player) {
		//Find the vector from (current door -> player position)
		Vec3d currentDifference = player.getPos().subtract(Vec3d.ofBottomCenter(departureGateway.doorTopPos));
		Vec3d velocity = player.getVelocity();
		float yawAdd = 0;
		
		//Rotate that vector according to the difference in angle between the two doors
		if(facing != departureGateway.facing) {
			if(facing.rotateYClockwise() == departureGateway.facing) {
				currentDifference = new Vec3d(currentDifference.z, currentDifference.y, -currentDifference.x);
				velocity =          new Vec3d(velocity.z,          velocity.y,          -velocity.x);
				yawAdd = 270;
			} else if(facing.getOpposite() == departureGateway.facing) {
				currentDifference = new Vec3d(-currentDifference.x, currentDifference.y, -currentDifference.z);
				velocity =          new Vec3d(-velocity.x,          velocity.y,          -velocity.z);
				yawAdd = 180;
			} else {
				currentDifference = new Vec3d(-currentDifference.z, currentDifference.y, currentDifference.x);
				velocity =          new Vec3d(-velocity.z,          velocity.y,          velocity.x);
				yawAdd = 90;
			}
		}
		
		//Apply that vector to the current door position
		Vec3d arrivalPos = currentDifference.add(Vec3d.ofBottomCenter(doorTopPos));
		
		//Send the player off
		player.setPosition(arrivalPos); //this one updates the boundingbox as well
		player.setYaw(player.getYaw() + yawAdd);
		player.setVelocity(velocity);
		
		player.resetPosition(); //sets prevX/Y/Z, prevYaw, etc. Makes the renderer look nicer & no headsnap
		//misc yaws, prevents funky head snaps and stuff
		player.bodyYaw = player.bodyYaw + yawAdd;
		player.prevBodyYaw = player.bodyYaw;
		player.headYaw = player.headYaw + yawAdd;
		player.prevHeadYaw = player.headYaw;
		
		if(player instanceof ServerPlayerEntity splayer) {
			//Make the ServerPlayNetworkHandler agree with that position. Makes you not rubberband
			splayer.networkHandler.syncWithPlayerPosition();
		}
		
		BlockState departureDoorState = world.getBlockState(departureGateway.doorTopPos);
		BlockState arrivalDoorState = world.getBlockState(doorTopPos);
		
		//On the client, if the destination chunk is not currently loaded, this check fails.
		if(departureDoorState.getBlock() instanceof DoorBlock && arrivalDoorState.getBlock() instanceof DoorBlock) {
			//Sneakily update the hinge on the destination door to match the hinge of the source door
			//(makes it look better)
			DoorHinge deptHinge = departureDoorState.get(DoorBlock.HINGE);
			DoorHinge myHinge = arrivalDoorState.get(DoorBlock.HINGE);
			if(deptHinge != myHinge) {
				DoorUtil.sneakySwapHinge(world, doorTopPos, arrivalDoorState);
				arrivalDoorState = world.getBlockState(doorTopPos);
			}
			
			//Open the src and destination doors
			DoorUtil.sneakyOpenDoor(world, departureGateway.doorTopPos, departureDoorState);
			DoorUtil.silentlyOpenDoor(player, world, this.doorTopPos, arrivalDoorState);
			
			//Client code handles this differently by playing a special SoundEvent that follows the player around.
			//On the server, to *other players*, play the door opening sound from both doors.
			if(world instanceof ServerWorld sworld) {
				//playSound with a PlayerEntity argument skips that player
				sworld.playSound(player, departureGateway.doorTopPos, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.BLOCKS, 1f, world.random.nextFloat() * 0.1f + 0.9f);
				sworld.playSound(player,             this.doorTopPos, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.BLOCKS, 1f, world.random.nextFloat() * 0.1f + 0.9f);
			}
		}
	}
	
	@Override
	public int compareTo(@NotNull Gateway o) {
		return this.doorTopPos.compareTo(o.doorTopPos);
	}
	
	//Cannot use raw hashCode because Block hashcodes are object-identity based
	//so between server/client, they're gonna be different
	//Raw IDs are not *guaranteed* to be the same (see Reshifter, lol), but they *should* be
	//in well-behaved modpacks when clients have the same mod list as the server. Idk.
	public int checksum() {
		int checksum = doorTopPos.hashCode();
		checksum *= 31;
		
		checksum ^= Registry.BLOCK.getRawId(doorBlock);
		checksum *= 31;
		
		for(Block f : frame) {
			checksum ^= Registry.BLOCK.getRawId(f);
			checksum *= 31;
		}
		
		checksum ^= facing.ordinal();
		return checksum;
	}
}
