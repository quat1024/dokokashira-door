package agency.highlysuspect.dokokashiradoor.util;

import agency.highlysuspect.dokokashiradoor.Gateway;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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
	
	public boolean hasGatewayAt(BlockPos pos) {
		return containsKey(pos);
	}
	
	public void removeGatewayAt(BlockPos pos) {
		remove(pos);
	}
	
	public @Nullable Gateway getGatewayAt(BlockPos pos) {
		return get(pos);
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
		List<Gateway> list = toUnsortedList();
		list.sort(null);
		return list;
	}
	
	public List<Gateway> toUnsortedList() {
		return new ArrayList<>(values());
	}
	
	public int checksum() {
		int checksum = 0;
		
		for(Gateway g : values()) {
			checksum ^= g.checksum();
		}
		
		return checksum;
	}
}
