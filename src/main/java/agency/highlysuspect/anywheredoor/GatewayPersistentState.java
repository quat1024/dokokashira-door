package agency.highlysuspect.anywheredoor;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class GatewayPersistentState extends PersistentState {
	public GatewayPersistentState() {
		gateways = new ArrayList<>();
	}
	
	//Deserialization constructor
	private GatewayPersistentState(List<Gateway> gateways) {
		//Mutable copy
		this.gateways = new ArrayList<>(gateways);
	}
	
	private final List<Gateway> gateways;
	public static final Codec<GatewayPersistentState> CODEC = Gateway.CODEC.listOf().xmap(GatewayPersistentState::new, GatewayPersistentState::getGateways);
	
	public static GatewayPersistentState getFor(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(GatewayPersistentState::fromNbt, GatewayPersistentState::new, "anywheredoor-gateways");
	}
	
	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.put("Gateways", Util.writeNbt(CODEC, this));
		return nbt;
	}
	
	public static GatewayPersistentState fromNbt(NbtCompound nbt) {
		return Util.readNbtAllowPartial(CODEC, nbt.get("Gateways"));
	}
	
	public List<Gateway> getGateways() {
		return gateways;
	}
	
	public void addGateway(Gateway gateway) {
		gateways.removeIf(g -> g.doorTopPos().equals(gateway.doorTopPos()));
		gateways.add(gateway);
		markDirty();
	}
	
	public @Nullable Gateway findDifferentGateway(Gateway gateway, Random random) {
		List<Gateway> otherGateways = gateways.stream().filter(other -> other.equalButDifferentPositions(gateway)).collect(Collectors.toList());
		if(otherGateways.size() == 0) return null;
		else return otherGateways.get(random.nextInt(otherGateways.size()));
	}
	
	/**
	 * Look through the list of gateways and try to recreate them from the world.
	 * If the in-world gateway doesn't match my records, update my records.
	 * If the in-world gateway is not loaded, though, don't worry about it for now.
	 */
	public void validateLoadedGateways(ServerWorld world) {
		ListIterator<Gateway> gatewayerator = gateways.listIterator();
		while(gatewayerator.hasNext()) {
			Gateway gateway = gatewayerator.next();
			
			BlockPos ne = gateway.doorTopPos().add(1, 0, 1);
			BlockPos sw = gateway.doorTopPos().add(-1, 0, -1);
			if(!world.getChunkManager().isChunkLoaded(ne.getX() / 16, ne.getZ() / 16)) continue;
			if(!world.getChunkManager().isChunkLoaded(sw.getX() / 16, sw.getZ() / 16)) continue;
			
			Gateway fromWorld = Gateway.readFromWorld(world, gateway.doorTopPos());
			if(fromWorld == null) gatewayerator.remove();
			else if(!fromWorld.equals(gateway)) gatewayerator.set(fromWorld);
		}
	}
}
