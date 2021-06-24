package agency.highlysuspect.dokokashiradoor.tp;

import agency.highlysuspect.dokokashiradoor.Init;
import agency.highlysuspect.dokokashiradoor.gateway.GatewayMap;
import agency.highlysuspect.dokokashiradoor.util.ClientPlayNetworkHandlerExt;
import agency.highlysuspect.dokokashiradoor.util.Util;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DokoClientPlayNetworkHandler {
	private final Map<RegistryKey<World>, GatewayMap> gatewayStorage = new HashMap<>();
	private final IntList randomSeeds = new IntArrayList();
	
	@NotNull
	public static DokoClientPlayNetworkHandler get(ClientPlayerEntity player) {
		return ((ClientPlayNetworkHandlerExt) player.networkHandler).dokodoor$getExtension();
	}
	
	public GatewayMap getGatewaysFor(World world) {
		return getGatewaysFor(world.getRegistryKey());
	}
	
	public GatewayMap getGatewaysFor(RegistryKey<World> wkey) {
		return gatewayStorage.computeIfAbsent(wkey, __ -> new GatewayMap());
	}
	
	public boolean hasRandomSeeds() {
		return !randomSeeds.isEmpty();
	}
	
	public int popRandomSeed() {
		return randomSeeds.removeInt(0);
	}
	
	public int peekRandomSeed() {
		return randomSeeds.getInt(0);
	}
	
	public void fullGatewayUpdate(RegistryKey<World> key, GatewayMap value) {
		Init.LOGGER.warn("Performing a full-update of gateways. Normally delta-updates should suffice. This is probably a bug.");
		
		gatewayStorage.put(key, value);
	}
	
	public int deltaGatewayUpdate(RegistryKey<World> wkey, GatewayMap additions, GatewayMap removals) {
		GatewayMap map = getGatewaysFor(wkey);
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
}
