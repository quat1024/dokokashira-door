package agency.highlysuspect.dokokashiradoor.tp;

import agency.highlysuspect.dokokashiradoor.gateway.Gateway;
import agency.highlysuspect.dokokashiradoor.gateway.GatewayMap;
import agency.highlysuspect.dokokashiradoor.net.DokoClientNet;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
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
		
		return true;
	}
	
	private static boolean withinRangeAndLooking(World world, BlockPos pos, BlockState doorTop, PlayerEntity player) {
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
		
		//Based on reverse-engineering captureFrustum a little bit and tracing where the matrices come from
		Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
		
		//I don't even know if getBasicProjectionMatrix is the right yawn name
		double fov = MinecraftClient.getInstance().options.fov;
		Matrix4f projection = MinecraftClient.getInstance().gameRenderer.getBasicProjectionMatrix((float) fov);
		
		//I wish I knew what to call these matrices lmao. The code just calls this "matrix4f" or something
		Matrix4f bababooey = new Matrix4f();
		bababooey.loadIdentity();
		bababooey.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(camera.getPitch()));
		bababooey.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(camera.getYaw() + 180f));
		projection.multiply(bababooey);
		projection.invert();

		for(int i = 0; i < 4; i++) {
			float dx = i < 2 ?      -1 : 1;
			float dy = i % 2 == 0 ? -1 : 1;

			Vector4f vec = new Vector4f(dx, dy, 1f, 1.0f);
			
			vec.transform(projection); //idk why "transform", it's a matrix-vector product
			vec.normalizeProjectiveCoordinates();
			
			Vec3d worldSpace = new Vec3d(vec.getX(), vec.getY(), vec.getZ())
				.normalize()
				.multiply(3)
				.add(camera.getPos());
			
			BlockHitResult hit = world.raycast(new RaycastContext(
				camera.getPos(),
				worldSpace,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.ANY,
				player
			));
			
			if(hit.getType() == HitResult.Type.MISS) return false;
			
			//world.addImportantParticle(ParticleTypes.END_ROD, true, hit.getPos().x, hit.getPos().y, hit.getPos().z, 0, 0, 0);
			
			if(hit.getBlockPos() == null || !acceptableHitPositions.contains(hit.getBlockPos())) {
				return false;
			}
		}
		
		return true;
	}
}
