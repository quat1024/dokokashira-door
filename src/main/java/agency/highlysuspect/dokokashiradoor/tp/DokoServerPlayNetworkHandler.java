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
	
	//PROTOCOL:
	// When the gateway checksums have *changed* since last time:
	//  - compute and send a delta-update.
	//  - apply the delta to the player's image; 
	//  - increment inFlightGatewayDeltas;
	// When the player acknowledges a gateway checksum:
	//  - decrement inFlightPackets;
	//  - if inFlightGatewayDeltas is not zero: do nothing.
	//  - if inFlightGatewayDeltas is zero and the checksum is correct: do nothing.
	//  - if inFlightGatewayDeltas is zero and the checksum is incorrect: something went wrong.
	// When there are less than ten random seeds:
	//  - compute (10 - n) more random seeds;
	//  - increment inFlightRandomSeeds;
	//  - send N random seeds to the client.
	// When the player acknowledges a random-seed checksum:
	//  - decrement inFlightRandomSeeds;
	//  - if inFlightRandomSeeds is not zero: do nothing.
	//  - blah blah it's the same as above
	
	public boolean panicMode = false;
	
	private final Object2IntMap<RegistryKey<World>> lastGatewayChecksums = new Object2IntOpenHashMap<>();
	private int inFlightGatewayDeltas = 0;
	private final Map<RegistryKey<World>, GatewayMap> gatewayImages = new HashMap<>();
	
	private final IntList randomSeeds = new IntArrayList();
	private static final int RANDOM_SEED_BUFFER_SIZE = 10;
	private int inFlightRandomSeeds = 0;
	
	public void tick() {
		ServerWorld world = spnh.player.getServerWorld();
		world.getProfiler().push("DokoServerPlayNetworkHandler for " + spnh.player.getEntityName());
		tick0(world);
		world.getProfiler().pop();
	}
	
	private void tick0(ServerWorld world) {
		RegistryKey<World> wkey = world.getRegistryKey();
		GatewayPersistentState gps = GatewayPersistentState.getFor(world);
		int currentGatewayChecksum = gps.getChecksum();
		
		//Something is wrong in the delta-update machinery, so do the thing the delta-update machinery is supposed to make me not have to do.
		if(panicMode) {
			sendFullGatewayUpdate(world);
			DokoServerNet.setRandomSeeds(spnh.player, randomSeeds);
			panicMode = false;
			return;
		}
		
		//Is the player new to this dimension?
		if(!lastGatewayChecksums.containsKey(wkey)) { //They are.
			sendFullGatewayUpdate(world);
		} else {
			//If the gateways have changed since last tick, send a delta update.
			int lastChecksum = lastGatewayChecksums.getInt(wkey);
			if(currentGatewayChecksum != lastChecksum) { //They have.
				sendDeltaGatewayUpdate(world);
			}
		}
		
		//Set lastGatewayChecksum
		lastGatewayChecksums.put(wkey, currentGatewayChecksum);
		
		//Fill the bucket of randomSeeds, if there is any reason to do so
		if(randomSeeds.size() < RANDOM_SEED_BUFFER_SIZE) {
			int howMany = RANDOM_SEED_BUFFER_SIZE - randomSeeds.size();
			IntList moreSeeds = new IntArrayList(howMany);
			for(int i = 0; i < howMany; i++) {
				moreSeeds.add(world.random.nextInt());
			}
			
			randomSeeds.addAll(moreSeeds);
			DokoServerNet.addRandomSeeds(spnh.player, moreSeeds);
			inFlightRandomSeeds++;
		}
	}
	
	public void sendFullGatewayUpdate(ServerWorld world) {
		RegistryKey<World> wkey = world.getRegistryKey();
		GatewayPersistentState gps = GatewayPersistentState.getFor(world);
		
		//Send them a full update.
		DokoServerNet.sendFullGatewayUpdate(spnh.player, wkey, gps.getAllGateways());
		
		//Update the player's image.
		gatewayImages.put(wkey, gps.getAllGateways().copy());
	}
	
	public void sendDeltaGatewayUpdate(ServerWorld world) {
		RegistryKey<World> wkey = world.getRegistryKey();
		GatewayPersistentState gps = GatewayPersistentState.getFor(world);
		
		//Compute a delta update.
		GatewayMap known = gatewayImages.computeIfAbsent(wkey, __ -> new GatewayMap());
		GatewayMap.Delta diff = gps.getAllGateways().diffAgainst(known);
		
		//Send it.
		DokoServerNet.sendDeltaGatewayUpdate(spnh.player, wkey, diff.additions(), diff.removals());
		
		//Update the player's image.
		known.applyDelta(diff.additions(), diff.removals());
		
		inFlightGatewayDeltas++;
	}
	
	public void ackGatewayChecksum(ServerWorld world, int checksum) {
		inFlightGatewayDeltas--;
		if(inFlightGatewayDeltas < 0) inFlightGatewayDeltas = 0; //How'd that happen?
		
		//If many gateways change in a short period of time (causing a bunch of delta updates to be sent in quick succession),
		//the server might observe these intermediate values being acknowledged. That's okay.
		if(inFlightGatewayDeltas == 0) {
			//See if the player responded with the correct checksum.
			int correctChecksum = GatewayPersistentState.getFor(world).getChecksum();
			if(correctChecksum != checksum) {
				//Bzzt, wrong.
				//This could happen if a packet got lost somewhere, a delta got misapplied,
				//or (regretfully) unfortunate timing (send delta -> gateways change -> receive acknowledgement before sending another delta)
				Init.LOGGER.warn("{}: Expected gateway checksum {}, but they replied with {}.", spnh.player.getEntityName(), correctChecksum, checksum);
				panicMode = true;
			}
		}
	}
	
	public boolean hasRandomSeed() {
		return !randomSeeds.isEmpty();
	}
	
	public int popRandomSeed() {
		return randomSeeds.removeInt(0);
	}
	
	public void ackRandomSeedChecksum(int checksum) {
		inFlightRandomSeeds--;
		if(inFlightRandomSeeds < 0) inFlightRandomSeeds = 0;
		
		if(inFlightRandomSeeds == 0) {
			int correctChecksum = Util.checksumIntList(randomSeeds);
			if(correctChecksum != checksum) {
				Init.LOGGER.warn("{}: Expected random-seed checksum {}, but they replied with {}.", spnh.player.getEntityName(), correctChecksum, checksum);
				panicMode = true;
			}
		}
	}
}
