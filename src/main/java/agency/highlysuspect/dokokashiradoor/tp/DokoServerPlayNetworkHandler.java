package agency.highlysuspect.dokokashiradoor.tp;

import agency.highlysuspect.dokokashiradoor.Init;
import agency.highlysuspect.dokokashiradoor.gateway.GatewayMap;
import agency.highlysuspect.dokokashiradoor.gateway.GatewayPersistentState;
import agency.highlysuspect.dokokashiradoor.net.DokoServerNet;
import agency.highlysuspect.dokokashiradoor.util.ServerPlayNetworkHandlerExt;
import agency.highlysuspect.dokokashiradoor.util.Util;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
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
	public DokoServerPlayNetworkHandler(ServerPlayNetworkHandler spnh) {
		this.spnh = spnh;
	}
	
	public static DokoServerPlayNetworkHandler getFor(ServerPlayerEntity player) {
		return ((ServerPlayNetworkHandlerExt) player.networkHandler).dokodoor$getExtension();
	}
	
	private final ServerPlayNetworkHandler spnh;
	
	//Gateways that we're sure the client knows about.
	private final Map<RegistryKey<World>, GatewayMap> knownGateways = new HashMap<>();
	
	//When the client receives a request to delta-patch its gateway map, it responds with a checksum of its patched gateway map.
	//This verifies that the delta-patch is applied correctly, and the server/client really do have the same picture of the gateways.
	//If the checksum is not correct, the server responds with a full copy of the (potentially very large) gateway map.
	//This is also useful as a quick-and-dirty "is the client's picture of the gateways up-to-date?" check, without needing to perform
	//an expensive map-difference operation. 
	private final Object2IntMap<RegistryKey<World>> acknowledgedChecksums = new Object2IntOpenHashMap<>();
	
	//The seeds used to randomly choose a door to teleport to are known ahead-of-time and synced to the client.
	//This helps the client correctly predict which door is the destination.
	private final IntList randomSeeds = new IntArrayList();
	private static final int RANDOM_SEED_BUFFER_SIZE = 10;
	
	//How many gateway-ack packets I expect to receive from the player.
	//Consider the following situation:
	// Send delta-update A; checksum = AAA
	// Send delta-update B; checksum = BBB
	// Recv ack A; client sent AAA, expected checksum BBB, mismatch (*)
	// Recv ack B; client sent BBB, expected checksum BBB, correct
	//This number is incremented when sending a delta-update, decremented when receiving one,
	//and errors like (*) are ignored if the number is not 0, i.e. there's some in-flight checksum packets.
	//They might be totally benign intermediate values.
	private int pendingGatewayChecksums = 0;
	
	//How many randomseed-ack packets I expect to receive from the player.
	private int pendingRandomSeedChecksums = 0;
	
	public void tick() {
		ServerWorld world = spnh.player.getServerWorld();
		
		//spread players out more-or-less randomly within the 20-tick window 
		if((world.getTime() + spnh.player.getId()) % 20 == 0) {
			world.getProfiler().push("DokoServerPlayNetworkHandler for " + spnh.player.getEntityName());
			
			RegistryKey<World> wkey = world.getRegistryKey();
			
			GatewayPersistentState gps = GatewayPersistentState.getFor(world);
			int ackChecksum = acknowledgedChecksums.getInt(wkey);
			
			if(gps.getChecksum() != ackChecksum) {
				sendDeltaGatewayUpdate(world);
			}
			
			fillRandomSeeds(world);
			
			world.getProfiler().pop();
		}
	}
	
	public void ackGatewayChecksum(ServerWorld world, int checksum) {
		GatewayPersistentState gps = GatewayPersistentState.getFor(world);
		this.acknowledgedChecksums.put(world.getRegistryKey(), checksum);
		
		pendingGatewayChecksums--;
		if(pendingGatewayChecksums < 0) pendingGatewayChecksums = 0;
		
		if(pendingGatewayChecksums == 0) {
			if(gps.getChecksum() == checksum) {
				//The player replied with the correct checksum, so I'm pretty sure they know the correct gateway map.
				knownGateways.put(world.getRegistryKey(), gps.getAllGateways().copy());
			} else {
				Init.LOGGER.warn("{}: Expected gateway checksum {}, but they replied with {}. Sending them a full gateway update.", spnh.player.getEntityName(), gps.getChecksum(), checksum);
				sendFullGatewayUpdate(world);
			}
		}
	}
	
	public void onDimensionChange(ServerWorld destination) {
		sendDeltaGatewayUpdate(destination);
	}
	
	public void sendFullGatewayUpdate(ServerWorld world) {
		RegistryKey<World> wkey = world.getRegistryKey();
		GatewayPersistentState gps = GatewayPersistentState.getFor(world);
		
		DokoServerNet.sendFullGatewayUpdate(spnh.player, wkey, gps.getAllGateways());
	}
	
	public void sendDeltaGatewayUpdate(ServerWorld world) {
		ServerPlayerEntity player = spnh.player;
		RegistryKey<World> wkey = world.getRegistryKey();
		GatewayPersistentState gps = GatewayPersistentState.getFor(world);
		
		GatewayMap known = knownGateways.computeIfAbsent(wkey, __ -> new GatewayMap());
		GatewayMap.Delta diff = gps.getAllGateways().diffAgainst(known);
		
		DokoServerNet.sendDeltaGatewayUpdate(player, wkey, diff.additions(), diff.removals());
		pendingGatewayChecksums++;
	}
	
	public boolean hasRandomSeed() {
		return !randomSeeds.isEmpty();
	}
	
	public int popRandomSeed() {
		return randomSeeds.removeInt(0);
	}
	
	public void fillRandomSeeds(ServerWorld world) {
		if(randomSeeds.size() < RANDOM_SEED_BUFFER_SIZE) {
			int howMany = RANDOM_SEED_BUFFER_SIZE - randomSeeds.size();
			IntList moreSeeds = new IntArrayList(howMany);
			for(int i = 0; i < howMany; i++) {
				moreSeeds.add(world.random.nextInt());
			}
			
			randomSeeds.addAll(moreSeeds);
			DokoServerNet.addRandomSeeds(spnh.player, moreSeeds);
			pendingRandomSeedChecksums++;
		}
	}
	
	public void ackRandomSeedChecksum(int checksum) {
		int seedChecksum = Util.checksumIntList(randomSeeds);
		
		pendingRandomSeedChecksums--;
		if(pendingRandomSeedChecksums < 0) pendingRandomSeedChecksums = 0;
		
		if(pendingRandomSeedChecksums == 0 && seedChecksum != checksum) {
			Init.LOGGER.warn("{}: Expected random-seed checksum {}, but they replied with {}. Sending them a full random-seed update.", spnh.player.getEntityName(), seedChecksum, checksum);
			DokoServerNet.setRandomSeeds(spnh.player, randomSeeds);
		}
	}
}
