package agency.highlysuspect.dokokashiradoor.util;

import agency.highlysuspect.dokokashiradoor.Gateway;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

public class GatewayList extends ArrayList<Gateway> implements RandomSelectable<Gateway> {
	public GatewayList() {
		super();
	}
	
	public GatewayList(@NotNull Collection<? extends Gateway> c) {
		super(c);
		sort();
	}
	
	public static final Codec<GatewayList> CODEC = Gateway.CODEC.listOf().xmap(GatewayList::new, Function.identity());
	
	private void sort() {
		sort(null); //natural ordering
	}
	
	private boolean isSorted() {
		ArrayList<Gateway> clone = new ArrayList<>(this);
		sort();
		return clone.equals(this);
	}
	
	@Override
	public Gateway pickRandom(Random random) {
		return get(random.nextInt(size()));
	}
	
	@Override
	public @Nullable Gateway removeRandomIf(Random random, Predicate<Gateway> test) {
		int i = random.nextInt(size());
		Gateway g = get(i);
		if(test.test(g)) {
			remove(i);
			return g;
		} else return null;
	}
	
	//Removal methods aren't overridden. If a list is sorted, it's still sorted when you remove elements.
	
	@Override
	public boolean add(Gateway g) {
		boolean add = super.add(g);
		sort();
		return add;
	}
	
	@Override
	public void add(int index, Gateway element) {
		super.add(index, element);
		sort();
	}
	
	@Override
	public boolean addAll(Collection<? extends Gateway> c) {
		boolean addAll = super.addAll(c);
		if(addAll) sort();
		return addAll;
	}
	
	@Override
	public boolean addAll(int index, Collection<? extends Gateway> c) {
		boolean addAll = super.addAll(index, c);
		if(addAll) sort();
		return addAll;
	}
}
