package agency.highlysuspect.dokokashiradoor.gateway;

import agency.highlysuspect.dokokashiradoor.util.CodecCrap;
import agency.highlysuspect.dokokashiradoor.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.chunk.ChunkManager;

import java.util.ArrayList;

public class GatewayPersistentState extends PersistentState {
	public GatewayPersistentState() {
		gateways = new GatewayMap();
		knownDoors = new ObjectOpenHashSet<>();
	}
	
	private final GatewayMap gateways;
	private int gatewayChecksum;
	private final ObjectOpenHashSet<BlockPos> knownDoors;
	
	public static final Codec<GatewayPersistentState> CODEC = RecordCodecBuilder.create(i -> i.group(
		GatewayMap.CODEC.fieldOf("gateways").forGetter(gps -> gps.gateways),
		CodecCrap.objectOpenHashSetCodec(BlockPos.CODEC).fieldOf("knownDoors").forGetter(gps -> gps.knownDoors)
	).apply(i, GatewayPersistentState::new));
	
	//Deserialization constructor
	private GatewayPersistentState(GatewayMap gateways, ObjectOpenHashSet<BlockPos> knownDoors) {
		this.gateways = gateways;
		this.knownDoors = knownDoors;
		
		gatewayChecksum = gateways.checksum();
	}
	
	public static GatewayPersistentState getFor(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(GatewayPersistentState::fromNbt, GatewayPersistentState::new, "dokokashira-doors");
	}
	
	public void tick(ServerWorld world) {
		if(world.getTime() % 5 == 0) {
			world.getProfiler().push("dokodoor tick");
			
			ChunkManager cm = world.getChunkManager();
			
			upkeepDoors(world, cm);
			upkeepGateways(world, cm);
			
			world.getProfiler().pop();
		}
	}
	
	private void upkeepDoors(ServerWorld world, ChunkManager cm) {
		world.getProfiler().swap("upkeepDoors");
		knownDoors.removeIf(pos -> {
			if(!Util.isPositionAndNeighborsLoaded(cm, pos)) return false;
			
			//Remove knownDoors that aren't doors anymore
			BlockState here = world.getBlockState(pos);
			if(!(here.getBlock() instanceof DoorBlock)) return true;
			if(here.get(DoorBlock.HALF) != DoubleBlockHalf.UPPER) return true;
			
			//Check if there is a gateway at this door
			Gateway inWorld = Gateway.readFromWorld(world, pos);
			if(inWorld != null && !gateways.contains(inWorld)) {
				putGateway(inWorld);
			}
			
			return false;
		});
	}
	
	private void upkeepGateways(ServerWorld world, ChunkManager cm) {
		world.getProfiler().swap("upkeepGateways");
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
		gatewayChecksum = gateways.checksum();
		markDirty();
	}
	
	private void removeGatewayAt(BlockPos pos) {
		removeGateway(gateways.getGatewayAt(pos));
	}
	
	private void removeGateway(Gateway gateway) {
		gateways.removeGateway(gateway);
		gatewayChecksum = gateways.checksum();
		markDirty();
	}
	
	public void helloDoor(ServerWorld world, BlockPos doorTopPos) {
		knownDoors.add(doorTopPos);
		
		if(Util.isPositionAndNeighborsLoaded(world.getChunkManager(), doorTopPos)) {
			Gateway fromWorld = Gateway.readFromWorld(world, doorTopPos);
			if(fromWorld == null) {
				gateways.remove(doorTopPos);
			}
			
			Gateway known = gateways.getGatewayAt(doorTopPos);
			if(known == null || !known.equals(fromWorld)) {
				putGateway(fromWorld);
			}
		}
	}
	
	//Serialization stuff
	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.put("Gateways", CodecCrap.writeNbt(CODEC, this));
		nbt.putInt("DokoDataVersion", 1);
		return nbt;
	}
	
	public static GatewayPersistentState fromNbt(NbtCompound nbt) {
		int dataVersion = nbt.contains("DokoDataVersion") ? nbt.getInt("DokoDataVersion") : 0;
		//TODO: Switch on dataVersion when there are changes to the format.
		// Nooooot worth it to make a whole DFU-based updater thingie.
		// Just keep the old Codecs lying around.
		return CodecCrap.readNbtAllowPartial(CODEC, nbt.get("Gateways"));
	}
	
	public GatewayMap getAllGateways() {
		return gateways;
	}
	
	ObjectOpenHashSet<BlockPos> getAllKnownDoorsInternal() { //for codecstuff I guess
		return knownDoors;
	}
	
	public int getChecksum() {
		return gatewayChecksum;
	}
}
