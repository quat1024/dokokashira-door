package agency.highlysuspect.dokokashiradoor.gateway;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GatewayMap extends Object2ObjectOpenHashMap<BlockPos, Gateway> {
	public GatewayMap() {
		super();
	}
	
	public GatewayMap(List<? extends Gateway> l) { 
		this();
		l.forEach(this::addGateway);
	}
	
	public static final Codec<GatewayMap> CODEC = Gateway.CODEC.listOf().xmap(GatewayMap::new, GatewayMap::toUnsortedList);
	
	public void addGateway(Gateway g) {
		put(g.doorTopPos(), g);
	}
	
	public void removeGateway(Gateway g) {
		remove(g.doorTopPos());
	}
	
	public @Nullable Gateway getGatewayAt(BlockPos pos) {
		return get(pos);
	}
	
	public boolean contains(Gateway other) {
		//containsValue is a linear search
		return containsKey(other.doorTopPos()) && get(other.doorTopPos()).equals(other);
	}
	
	/**
	 * containsValue is a linear search over the values, and should not be used
	 * @see GatewayMap#contains(Gateway) 
	 */
	@Override
	@Deprecated
	public boolean containsValue(Object v) {
		return super.containsValue(v);
	}
	
	public GatewayMap copy() {
		//Gateway objects are immutable records, no deep copy is needed
		GatewayMap copy = new GatewayMap();
		copy.putAll(this);
		return copy;
	}
	
	//Kinda jank
	public void removeIf(Predicate<Gateway> pred) {
		if(key == null) return;
		
		ArrayList<BlockPos> toRemove = new ArrayList<>();
		
		forEach((blockPos, gateway) -> {
			if(pred.test(gateway)) toRemove.add(blockPos);
		});
		
		for(BlockPos p : toRemove) {
			remove(p);
		}
	}
	
	public List<Gateway> toSortedList() {
		//Alright, yeah. This is kinda of a weird collection.
		//It's a map, to look up gateways by their position,
		//it's a set, to look up gateways by their identity,
		//and it's a sorted list, for random-number accessible indexing, all at the same time.
		//Here lies my hopes and dreams: i simply sort the list on demand, since you don't need the
		//actually sorted-list view very often.
		List<Gateway> list = toUnsortedList();
		list.sort(null);
		return list;
	}
	
	public List<Gateway> toUnsortedList() {
		return new ArrayList<>(values());
	}
	
	public int checksum() {
		int checksum = 0;
		
		//Sorting the list first isn't needed since XOR commutes.
		for(Gateway g : values()) {
			checksum ^= g.checksum();
		}
		
		return checksum;
	}
	
	//we have multiple return at home :relieved:
	public static record Delta(GatewayMap additions, GatewayMap removals) {}
	public Delta diffAgainst(GatewayMap other) {
		if(this.equals(other)) return new Delta(new GatewayMap(), new GatewayMap());
		
		//Gateways that exist in this map, but not the other map, should be *added to* the other map.
		GatewayMap additions = copy();
		additions.removeIf(other::contains);
		
		//Gateways that exist in the other map, but not in this one, should be *removed from* this map.
		GatewayMap removals = other.copy();
		removals.removeIf(this::contains);
		
		return new Delta(additions, removals);
	}
	
	public void applyDelta(GatewayMap additions, GatewayMap removals) {
		putAll(additions);
		removeIf(removals::contains);
	}
	
	public @Nullable Gateway findDifferentGateway(Gateway other, Random random, int maxTries, Predicate<Gateway> test) {
		List<Gateway> otherGateways = toSortedList()
			.stream()
			.filter(x -> x.equalButDifferentPositions(other))
			.collect(Collectors.toList());
		
		for(int tries = 0; tries < maxTries; tries++) {
			if(otherGateways.size() == 0) return null;
			
			int i = random.nextInt(otherGateways.size());
			Gateway candidate = otherGateways.get(i);
			
			if(test.test(candidate)) {
				return candidate;
			} else {
				otherGateways.remove(i);
			}
		}
		
		return null;
	}
}
