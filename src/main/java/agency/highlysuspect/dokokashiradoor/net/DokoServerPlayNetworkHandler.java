package agency.highlysuspect.dokokashiradoor.net;

import agency.highlysuspect.dokokashiradoor.GatewayPersistentState;
import agency.highlysuspect.dokokashiradoor.Init;
import agency.highlysuspect.dokokashiradoor.util.GatewayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class DokoServerPlayNetworkHandler {
	//Gateways that we're sure the client knows about.
	private final Map<RegistryKey<World>, GatewayMap> knownGateways = new HashMap<>();
	
	//When the client receives a request to delta-patch its gateway map, it responds with a checksum of its complete gateway map.
	//This verifies that the delta-patch is applied correctly, and the server/client really do have the same picture of the gateways.
	//If the checksum is not correct, the server responds with a full copy of the (potentially very large) gateway map.
	//This is also useful as a quick-and-dirty "is the client's picture of the gateways up-to-date?" check, without needing to perform
	//an expensive map-difference operation. 
	private final Object2IntMap<RegistryKey<World>> acknowledgedChecksums = new Object2IntOpenHashMap<>();
	
	public void tick(ServerPlayNetworkHandler spnh) {
		ServerWorld world = spnh.player.getServerWorld();
		
		if(world.getTime() % 20 == 0) {
			RegistryKey<World> wkey = world.getRegistryKey();
			
			GatewayPersistentState gps = GatewayPersistentState.getFor(world);
			int ackChecksum = acknowledgedChecksums.getInt(wkey);
			
			if(gps.getChecksum() != ackChecksum) {
				sendDeltaUpdate(spnh, world);
			}
		}
	}
	
	public void recvChecksum(ServerPlayNetworkHandler spnh, ServerWorld world, int checksum) {
		GatewayPersistentState gps = GatewayPersistentState.getFor(world);
		this.acknowledgedChecksums.put(world.getRegistryKey(), checksum);
		
		if(gps.getChecksum() == checksum) {
			//The player replied with the correct checksum, so I'm pretty sure they know the correct gateway map.
			knownGateways.put(world.getRegistryKey(), gps.getAllGateways().copy());
		} else {
			Init.LOGGER.info("Recv incorrect checksum from {}: expected {}, they sent {}. Sending full update", spnh.player.getEntityName(), gps.getChecksum(), acknowledgedChecksums);
			sendFullUpdate(spnh, world);
		}
	}
	
	public void onDimensionChange(ServerPlayNetworkHandler spnh, ServerWorld destination) {
		sendDeltaUpdate(spnh, destination);
	}
	
	public void sendFullUpdate(ServerPlayNetworkHandler spnh, ServerWorld world) {
		//TODO: Throttle this
		RegistryKey<World> wkey = world.getRegistryKey();
		GatewayPersistentState gps = GatewayPersistentState.getFor(world);
		
		DokoServerNet.sendFullUpdate(spnh.player, wkey, gps.getAllGateways());
	}
	
	public void sendDeltaUpdate(ServerPlayNetworkHandler spnh, ServerWorld world) {
		//TODO: Throttle this
		ServerPlayerEntity player = spnh.player;
		RegistryKey<World> wkey = world.getRegistryKey();
		GatewayPersistentState gps = GatewayPersistentState.getFor(world);
		
		GatewayMap known = knownGateways.computeIfAbsent(wkey, __ -> new GatewayMap());
		GatewayMap.Delta diff = gps.getAllGateways().diffAgainst(known);
		
		DokoServerNet.sendDeltaUpdate(player, wkey, diff.additions(), diff.removals());
	}
}
