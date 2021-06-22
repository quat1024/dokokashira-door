package agency.highlysuspect.dokokashiradoor.client;

import agency.highlysuspect.dokokashiradoor.Gateway;
import agency.highlysuspect.dokokashiradoor.Init;
import agency.highlysuspect.dokokashiradoor.util.GatewayMap;
import agency.highlysuspect.dokokashiradoor.util.Util;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
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
	private final Map<RegistryKey<World>, GatewayMap> gatewayStorage = new HashMap<>();
	private final IntList randomSeeds = new IntArrayList();
	
	public void fullGatewayUpdate(RegistryKey<World> key, GatewayMap value) {
		Init.LOGGER.warn("Performing a full-update of gateways. Normally delta-updates should suffice. This is probably a bug.");
		
		gatewayStorage.put(key, value);
	}
	
	public int deltaGatewayUpdate(RegistryKey<World> key, GatewayMap additions, GatewayMap removals) {
		GatewayMap map = gatewayStorage.computeIfAbsent(key, __ -> new GatewayMap());
		map.applyDelta(additions, removals);
		return map.checksum();
	}
	
	public void fullRandomSeeds(IntList newSeeds) {
		Init.LOGGER.warn("Performing a full-update of random seeds. Normally delta-updates should suffice. This is probably a bug.");
		
		randomSeeds.clear();
		randomSeeds.addAll(newSeeds);
	}
	
	public int deltaRandomSeeds(IntList newSeeds) {
		randomSeeds.addAll(newSeeds);
		return Util.checksumIntList(randomSeeds);
	}
	
	public static Optional<ClientPlayerGatewayData> get() {
		if(MinecraftClient.getInstance().player == null) return Optional.empty();
		else return Optional.of(ClientPlayNetworkHandlerExt.cast(MinecraftClient.getInstance().player.networkHandler).dokodoor$getExtension());
	}
	
	/**
	 * @see agency.highlysuspect.dokokashiradoor.GatewayPersistentState#playerUseDoor(ServerWorld, BlockPos, ServerPlayerEntity) 
	 */
	public boolean predictDoorClient(World world, BlockPos doorTopPos, PlayerEntity player) {
		//If the player is currently interacting with a gateway
		Gateway thisGateway = Gateway.readFromWorld(world, doorTopPos);
		if(thisGateway == null) return false;
		
		//pop a random seed
		if(randomSeeds.isEmpty()) return false;
		
		Random random = new Random();
		random.setSeed(randomSeeds.removeInt(0));
		
		//find a matching gateway
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
