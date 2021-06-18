package agency.highlysuspect.dokokashiradoor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class GatewayPersistentState extends PersistentState {
	public GatewayPersistentState() {
		gateways = new Util.RandomSelectableMap<>();
		checkDoors = new Util.RandomSelectableSet<>();
	}
	
	//Deserialization constructor
	private GatewayPersistentState(List<Gateway> gateways, List<BlockPos> checkDoors) {
		//Mutable copy
		this.gateways = new Util.RandomSelectableMap<>(gateways, Gateway::doorTopPos);
		this.checkDoors = new Util.RandomSelectableSet<>(checkDoors);
	}
	
	private final Util.RandomSelectableMap<BlockPos, Gateway> gateways;
	private final Util.RandomSelectableSet<BlockPos> checkDoors;
	
	public static final Codec<GatewayPersistentState> CODEC = RecordCodecBuilder.create(i -> i.group(
		Gateway.CODEC.listOf().fieldOf("gateways").forGetter(gps -> gps.gateways.asList()),
		BlockPos.CODEC.listOf().fieldOf("checkDoors").forGetter(gps -> gps.checkDoors.asList())
	).apply(i, GatewayPersistentState::new));
	
	public static GatewayPersistentState getFor(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(GatewayPersistentState::fromNbt, GatewayPersistentState::new, "dokokashira-doors");
	}
	
	public void tick(ServerWorld world) {
		ServerChunkManager cm = world.getChunkManager();
		
		//Pick a random prospective door.
		@Nullable BlockPos removedCheckdoor = checkDoors.randomRemoveIf(world.random, pos -> {
			//If the prospective door is not loaded, don't worry about it right now
			if(!Util.isPositionAndNeighborsLoaded(cm, pos)) return false;
			
			//If the prospective door is loaded, make sure it's actually the top half of a door.
			BlockState state = world.getBlockState(pos);
			if(!(state.getBlock() instanceof DoorBlock) || state.get(DoorBlock.HALF) != DoubleBlockHalf.UPPER) {
				return true;
			}
			
			//If it's the top half of a door, try to upgrade it to a gateway.
			@Nullable Gateway g = Gateway.readFromWorld(world, pos);
			if(g != null) {
				addGateway(g);
				return true;
			}
			
			//Maybe a gateway will crop up here later.
			return false;
		});
		
		//Pick a random gateway.
		@Nullable Gateway removedGateway = gateways.randomRemoveIf(world.random, (pos, gateway) -> {
			//If the gateway is not loaded, don't worry about it right now.
			if(!Util.isPositionAndNeighborsLoaded(cm, pos)) return false;
			
			//If the gateway is loaded, make sure the gateway still exists in the world.
			Gateway fromWorld = gateway.recreate(world);
			if(fromWorld == null) {
				//The door might still be intact, so add to checkdoors.
				//(If the door is not intact, the checkdoors procedure will remove it naturally.)
				addCheckdoor(pos);
				return true;
			}
			
			//Also check that my model of the gateway is still correct.
			if(!fromWorld.equals(gateway)) {
				gateways.put(pos, fromWorld);
				return false;
			}
			
			//Looks good for now
			return false;
		});
		
		if(removedCheckdoor != null || removedGateway != null) {
			markDirty();
		}
	}
	
	public void addGateway(Gateway gateway) {
		gateways.put(gateway.doorTopPos(), gateway);
		markDirty();
	}
	
	public void removeGateway(Gateway gateway) {
		removeGatewayAt(gateway.doorTopPos());
	}
	
	public void removeGatewayAt(BlockPos doorTopPos) {
		gateways.remove(doorTopPos);
		checkDoors.add(doorTopPos);
		markDirty();
	}
	
	public void addCheckdoor(BlockPos pos) {
		checkDoors.add(pos);
		markDirty();
	}
	
	public @Nullable Gateway findDifferentGateway(ServerWorld world, Gateway gateway, Random random) {
		List<Gateway> otherGateways = gateways.values().stream().filter(other -> other.equalButDifferentPositions(gateway)).collect(Collectors.toList());
		
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
}
