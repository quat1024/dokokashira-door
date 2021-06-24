package agency.highlysuspect.dokokashiradoor.tp;

import agency.highlysuspect.dokokashiradoor.gateway.Gateway;
import agency.highlysuspect.dokokashiradoor.gateway.GatewayMap;
import agency.highlysuspect.dokokashiradoor.net.DokoClientNet;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;

public class ClientDoorTp {
	public static boolean playerUseDoorClient(World world, BlockPos leftFromPos, BlockState doorTop, PlayerEntity player) {
		//Preconditions!
		if(!MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) return false; // ^,,^
		if(!(player instanceof ClientPlayerEntity cpe)) return false;
		
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
	
	private static boolean withinRangeAndLooking(World world, BlockPos leftFromPos, BlockState doorTop, PlayerEntity player) {
		//TODO: The fancy ass raycasts
		
		//Standing in the same block as the door. No player-look check, because if you're able to stand this close and
		//activate the door in the first place, your entire screen is probably taken up by the door frame.
		Box interiorBox = Box.of(Vec3d.ofBottomCenter(leftFromPos), 0.3d, 0.3d, 0.3d);
		if(world.getOtherEntities(null, interiorBox).contains(player)) return true;
		
		//The direction from the door's model towards the middle of the block.
		Direction toInside = doorTop.get(DoorBlock.FACING);
		
		//This box pokes outside the front of the door by a tiny bit.
		//If you're standing in this box, you need to be looking vaguely at the door.
		Box exteriorBox = Box.of(Vec3d.ofBottomCenter(leftFromPos), 0.1d, 0.1d, 0.1d)
			.offset(Vec3d.of(toInside.getVector()).multiply(-0.5d));
		
		return world.getOtherEntities(null, exteriorBox).contains(player);
	}
}
