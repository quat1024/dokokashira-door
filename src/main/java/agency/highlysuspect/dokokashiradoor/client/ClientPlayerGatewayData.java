package agency.highlysuspect.dokokashiradoor.client;

import agency.highlysuspect.dokokashiradoor.Gateway;
import agency.highlysuspect.dokokashiradoor.util.GatewayMap;
import agency.highlysuspect.dokokashiradoor.util.PlayerEntityExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ClientPlayerGatewayData {
	//private GatewayMap gateways;
	private final Map<RegistryKey<World>, GatewayMap> gatewayStorage = new HashMap<>();
	
	public int fullGatewayUpdate(RegistryKey<World> key, GatewayMap value) {
		gatewayStorage.put(key, value);
		return value.checksum();
	}
	
	public int deltaGatewayUpdate(RegistryKey<World> key, GatewayMap additions, GatewayMap removals) {
		GatewayMap map = gatewayStorage.computeIfAbsent(key, __ -> new GatewayMap());
		map.applyDelta(additions, removals);
		return map.checksum();
	}
	
	public static Optional<ClientPlayerGatewayData> get() {
		if(MinecraftClient.getInstance().player == null) return Optional.empty();
		else return Optional.of(ClientPlayNetworkHandlerExt.cast(MinecraftClient.getInstance().player.networkHandler).doko$getData());
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
		@Nullable Gateway other = findDifferentGatewayClient(world, thisGateway, random);
		
		if(other == null) return false;
		
		other.arrive(world, thisGateway, player);
		return true;
	}
	
	/**
	 * @see agency.highlysuspect.dokokashiradoor.GatewayPersistentState#findDifferentGateway(ServerWorld, Gateway, Random)
	 */
	private @Nullable Gateway findDifferentGatewayClient(World world, Gateway thisGateway, Random random) {
		
		List<Gateway> otherGateways = gatewayStorage
			.computeIfAbsent(world.getRegistryKey(), __ -> new GatewayMap())
			.toSortedList()
			.stream()
			.filter(other -> other.equalButDifferentPositions(thisGateway))
			.collect(Collectors.toList());
		
		//Can't perform a double-check that the gateway is correct on the client
		if(otherGateways.size() == 0) return null;
		else return otherGateways.get(random.nextInt(otherGateways.size()));
	}
}
