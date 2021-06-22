package agency.highlysuspect.dokokashiradoor;

import agency.highlysuspect.dokokashiradoor.net.DokoServerPlayNetworkHandler;
import agency.highlysuspect.dokokashiradoor.util.GatewayMap;
import agency.highlysuspect.dokokashiradoor.util.FunnySet;
import agency.highlysuspect.dokokashiradoor.util.ServerPlayNetworkHandlerExt;
import agency.highlysuspect.dokokashiradoor.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.chunk.ChunkManager;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class GatewayPersistentState extends PersistentState {
	public GatewayPersistentState() {
		gateways = new GatewayMap();
		knownDoors = new FunnySet<>();
	}
	
	private final GatewayMap gateways;
	private int gatewayChecksum;
	private final FunnySet<BlockPos> knownDoors;
	
	public static final Codec<GatewayPersistentState> CODEC = RecordCodecBuilder.create(i -> i.group(
		GatewayMap.CODEC.fieldOf("gateways").forGetter(gps -> gps.gateways),
		FunnySet.codec(BlockPos.CODEC).fieldOf("knownDoors").forGetter(gps -> gps.knownDoors)
	).apply(i, GatewayPersistentState::new));
	
	//Deserialization constructor
	private GatewayPersistentState(GatewayMap gateways, FunnySet<BlockPos> knownDoors) {
		this.gateways = gateways;
		this.knownDoors = knownDoors;
		
		gatewayChecksum = gateways.checksum();
	}
	
	public static GatewayPersistentState getFor(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(GatewayPersistentState::fromNbt, GatewayPersistentState::new, "dokokashira-doors");
	}
	
	public void tick(ServerWorld world) {
		ChunkManager cm = world.getChunkManager();
		
		upkeepDoors(world, cm);
		upkeepGateways(world, cm);
	}
	
	private void upkeepDoors(ServerWorld world, ChunkManager cm) {
		knownDoors.removeIf(pos -> {
			if(!Util.isPositionAndNeighborsLoaded(cm, pos)) return false;
			
			//Remove knownDoors that aren't doors anymore
			BlockState here = world.getBlockState(pos);
			if(!(here.getBlock() instanceof DoorBlock)) return true;
			if(here.get(DoorBlock.HALF) != DoubleBlockHalf.UPPER) return true;
			
			//Check if there is a gateway at this door
			Gateway inWorld = Gateway.readFromWorld(world, pos);
			if(inWorld != null && !gateways.containsValue(inWorld)) {
				putGateway(inWorld);
			}
			
			return false;
		});
	}
	
	private void upkeepGateways(ServerWorld world, ChunkManager cm) {
		//This sucks a lot, sorry.
		//I might modify the map while iterating over it, so I copy all the keys.
		//I don't think fastutil maps throw CMEs in the name of performance, and I don't wanna find out what happens.
		for(BlockPos pos : new ArrayList<>(gateways.keySet())) {
			if(!Util.isPositionAndNeighborsLoaded(cm, pos)) continue;
			
			Gateway gateway = gateways.getGatewayAt(pos);
			assert gateway != null; //Map doesn't contain null values
			
			//If the gateway doesn't exist in the world anymore, remove it
			Gateway fromWorld = gateway.recreate(world);
			if(fromWorld == null) {
				removeGatewayAt(pos);
				continue;
			}
			
			//If my model of the gateway is out-of-date, update it
			if(!gateway.equals(fromWorld)) {
				putGateway(fromWorld);
			}
		}
	}
	
	private void putGateway(Gateway gateway) {
		gateways.addGateway(gateway);
		
		//gatewayChecksum ^= gateway.checksum();
		//assert gatewayChecksum == gateways.checksum();
		gatewayChecksum = gateways.checksum();
		
		markDirty();
	}
	
	private void removeGatewayAt(BlockPos pos) {
		removeGateway(gateways.getGatewayAt(pos));
	}
	
	private void removeGateway(Gateway gateway) {
		gateways.removeGateway(gateway);
		
		gatewayChecksum = gateways.checksum(); //TODO delta update
		
		markDirty();
	}
	
	public void helloDoor(ServerWorld world, BlockPos doorTopPos) {
		knownDoors.add(doorTopPos);
		
		if(Util.isPositionAndNeighborsLoaded(world.getChunkManager(), doorTopPos)) {
			Gateway g = Gateway.readFromWorld(world, doorTopPos);
			if(g != null && !g.equals(gateways.getGatewayAt(doorTopPos))) {
				putGateway(g);
			}
		}
	}
	
	public boolean playerUseDoor(ServerWorld world, BlockPos doorTopPos, ServerPlayerEntity player) {
		//If the player is interacting with a gateway
		Gateway thisGateway = Gateway.readFromWorld(world, doorTopPos);
		if(thisGateway == null) return false;
		
		if(!thisGateway.equals(gateways.getGatewayAt(doorTopPos))) {
			putGateway(thisGateway);
		}
		
		//Pop a random seed
		DokoServerPlayNetworkHandler ext = ServerPlayNetworkHandlerExt.cast(player.networkHandler).dokodoor$getExtension();
		if(!ext.hasRandomSeed()) return false;
		
		Random random = new Random();
		random.setSeed(ext.popRandomSeed());
		
		//find a matching gateway
		@Nullable Gateway other = findDifferentGateway(world, thisGateway, random);
		
		if(other == null) return false;
		
		//tp them to it
		other.arrive(world, thisGateway, player);
		
		return true;
	}
	
	public @Nullable Gateway findDifferentGateway(ServerWorld world, Gateway thisGateway, Random random) {
		List<Gateway> otherGateways = gateways
			.toSortedList()
			.stream()
			.filter(other -> other.equalButDifferentPositions(thisGateway))
			.collect(Collectors.toList());
		
		for(int tries = 0; tries < 10; tries++) {
			if(otherGateways.size() == 0) return null;
			
			//Make EXTRA sure that the gateway still exists in the world.
			int i = random.nextInt(otherGateways.size());
			Gateway other = otherGateways.get(i);
			Gateway recreation = other.recreate(world);
			if(other.equals(recreation)) {
				return other;
			} else {
				otherGateways.remove(i);
				removeGateway(other);
			}
		}
		
		return null;
	}
	
	//Serialization stuff
	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.put("Gateways", Util.writeNbt(CODEC, this));
		return nbt;
	}
	
	public static GatewayPersistentState fromNbt(NbtCompound nbt) {
		return Util.readNbtAllowPartial(CODEC, nbt.get("Gateways"));
	}
	
	public GatewayMap getAllGateways() {
		return gateways;
	}
	
	public int getChecksum() {
		return gatewayChecksum;
	}
}
