package agency.highlysuspect.dokokashiradoor.client;

import agency.highlysuspect.dokokashiradoor.Gateway;
import agency.highlysuspect.dokokashiradoor.util.GatewayMap;
import agency.highlysuspect.dokokashiradoor.util.PlayerEntityExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ClientPlayerGatewayData {
	private GatewayMap gateways;
	
	public @Nullable GatewayMap getGateways() {
		return gateways;
	}
	
	public void setGateways(GatewayMap gateways) {
		this.gateways = gateways;
	}
	
	public static ClientPlayerGatewayData get() {
		if(MinecraftClient.getInstance().player == null) return null;
		else return ClientPlayNetworkHandlerExt.cast(MinecraftClient.getInstance().player.networkHandler).doko$getData();
	}
	
	/**
	 * @see agency.highlysuspect.dokokashiradoor.GatewayPersistentState#playerUseDoor(ServerWorld, BlockPos, ServerPlayerEntity) 
	 */
	public boolean predictDoorClient(World world, BlockPos doorTopPos, PlayerEntity player) {
		//If the player is currently interacting with a gateway
		Gateway thisGateway = Gateway.readFromWorld(world, doorTopPos);
		if(thisGateway == null) return false;
		
		//find a matching gateway
		Random random = new Random();
		random.setSeed(PlayerEntityExt.cast(player).dokodoor$getGatewayRandomSeed());
		@Nullable Gateway other = findDifferentGatewayClient(thisGateway, random);
		
		if(other == null) return false;
		
		other.arrive(world, thisGateway, player);
		return true;
	}
	
	/**
	 * @see agency.highlysuspect.dokokashiradoor.GatewayPersistentState#findDifferentGateway(ServerWorld, Gateway, Random)
	 */
	private @Nullable Gateway findDifferentGatewayClient(Gateway thisGateway, Random random) {
		if(gateways == null) return null;
		
		List<Gateway> otherGateways = gateways
			.toSortedList()
			.stream()
			.filter(other -> other.equalButDifferentPositions(thisGateway))
			.collect(Collectors.toList());
		
		//Can't perform a double-check that the gateway is correct on the client
		if(otherGateways.size() == 0) return null;
		else return otherGateways.get(random.nextInt(otherGateways.size()));
	}
}
