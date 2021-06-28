package agency.highlysuspect.dokokashiradoor.tp;

import agency.highlysuspect.dokokashiradoor.client.MatrixCache;
import agency.highlysuspect.dokokashiradoor.client.OffsetEntityTrackingSoundInstance;
import agency.highlysuspect.dokokashiradoor.gateway.Gateway;
import agency.highlysuspect.dokokashiradoor.gateway.GatewayMap;
import agency.highlysuspect.dokokashiradoor.net.DokoClientNet;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ClientDoorTp {
	public static boolean playerUseDoorClient(World world, BlockPos leftFromPos, BlockState doorTop, PlayerEntity player) {
		if(!(player instanceof ClientPlayerEntity cpe)) return false; //Mainly a pattern-cast but also rejects OtherClientPlayers
		DokoClientPlayNetworkHandler cpgd = DokoClientPlayNetworkHandler.get(cpe);
		
		GatewayMap gateways = cpgd.getGatewaysFor(world);
		if(gateways.isEmpty()) return false;
		
		@SuppressWarnings("SuspiciousNameCombination") //leftFromPos - Spurious warning, but good effort intellij
		Gateway thisGateway = Gateway.readFromWorld(world, leftFromPos);
		if(thisGateway == null) return false;
		
		if(!cpgd.hasRandomSeeds()) return false;
		Random random = new Random(cpgd.peekRandomSeed());
		
		Gateway destination = gateways.findDifferentGateway(thisGateway, random, 1, gateway -> true);
		if(destination == null) return false;
		
		if(!withinRangeAndLooking(world, leftFromPos, doorTop, player)) return false;
		
		/////// Ok let's go!!!!!!
		
		//1. Send a "I wish to teleport" packet.
		//This door-teleport packet will be sent *before* the client starts additional move packets from the new location.
		//We take advantage of Minecraft's protocol being over TCP, I guess - the door tp can be handled before
		//the player gets rubberbanded for "moving too quickly".
		DokoClientNet.sendDoorTeleport(leftFromPos, destination.doorTopPos());
		
		//2. Consume this random seed
		cpgd.popRandomSeed();
		
		//3. Move the player clientside
		destination.arrive(world, thisGateway, player);
		
		//4. Play the special clientside door-opening sound. This one follows the player around as they teleport.
		MinecraftClient.getInstance().getSoundManager().play(OffsetEntityTrackingSoundInstance.doorOpen(player, destination.doorTopPos(), null, world.random));
		
		return true;
	}
	
	private static boolean withinRangeAndLooking(World world, BlockPos pos, BlockState doorTop, PlayerEntity player) {
		if(MatrixCache.PROJECTION_MATRIX == null || MatrixCache.VIEW_MATRIX == null) return false;
		
		//The door block and the 8 blocks surrounding it
		Direction backwards = doorTop.get(DoorBlock.FACING);
		Direction right = backwards.rotateYClockwise();
		Direction left = backwards.rotateYCounterclockwise();
		
		Set<BlockPos> acceptableHitPositions = new HashSet<>();
		acceptableHitPositions.add(pos);
		acceptableHitPositions.add(pos.down());
		acceptableHitPositions.add(pos.up());
		acceptableHitPositions.add(pos.offset(right));
		acceptableHitPositions.add(pos.offset(right).down());
		acceptableHitPositions.add(pos.offset(right).up());
		acceptableHitPositions.add(pos.offset(left));
		acceptableHitPositions.add(pos.offset(left).down());
		acceptableHitPositions.add(pos.offset(left).up());
		
		//Location of the camera in world space. Accounts for things like f5 mode and whatnot
		Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
		
		//The magic matrix.
		//If you multiply it by a vec4 where X and Y are a position in normalized device coordinates, Z is whatever, and W is 1,
		//you will get the vector from the player's camera, to that point in world space.
		//Normalize that (to clamp the length) then add the camera position back, and it's suitable for raytracing through. Neat.
		
		//Based on reverse-engineering captureFrustum a little bit and tracing where the matrices come from
		//If I had real computer graphics experience this probably would have came a little easier :)
		Matrix4f screenToWorldDeltaMat = MatrixCache.PROJECTION_MATRIX.copy();
		screenToWorldDeltaMat.multiply(MatrixCache.VIEW_MATRIX);
		screenToWorldDeltaMat.invert();

		for(int i = 0; i < 4; i++) {
			float dx = i < 2 ?      -1 : 1;
			float dy = i % 2 == 0 ? -1 : 1;

			Vector4f vec = new Vector4f(dx, dy, 1f, 1f);
			
			vec.transform(screenToWorldDeltaMat); //idk why "transform", it's a matrix-vector product
			vec.normalizeProjectiveCoordinates();
			
			Vec3d worldSpace = new Vec3d(vec.getX(), vec.getY(), vec.getZ())
				.normalize()
				.multiply(3)
				.add(cameraPos);
			
			BlockHitResult hit = world.raycast(new RaycastContext(
				cameraPos,
				worldSpace,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.ANY,
				player
			));
			
			if(hit.getType() == HitResult.Type.MISS) return false;
			
			if(hit.getBlockPos() == null || !acceptableHitPositions.contains(hit.getBlockPos())) {
				return false;
			}
		}
		
		return true;
	}
}
