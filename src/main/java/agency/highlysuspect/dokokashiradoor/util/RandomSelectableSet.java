package agency.highlysuspect.dokokashiradoor.util;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class RandomSelectableSet<T> extends ObjectOpenHashSet<T> implements RandomSelectable<T> {
	public RandomSelectableSet() {
		super();
	}
	
	public RandomSelectableSet(Collection<T> t) {
		super(t);
	}
	
	public static <T> Codec<RandomSelectableSet<T>> codec(Codec<T> element) {
		return element.listOf().xmap(RandomSelectableSet::new, RandomSelectableSet::asList);
	}
	
	public @Nullable T pickRandom(Random random) {
		//Reaches into the guts of the ObjectOpenHashSet array.
		//Some of these entries are null, just because nothing happened to hash to that position.
		//Caling trim() will reduce the chances of hitting a null entry.
		return key[random.nextInt(key.length)];
	}
	
	@Override
	public @Nullable T removeRandomIf(Random random, Predicate<T> pred) {
		if(isEmpty()) return null;
		
		T rand = pickRandom(random);
		if(rand == null) return null;
		
		if(pred.test(rand)) {
			remove(rand);
			return rand;
		} else return null;
	}
	
	public List<T> asList() {
		return stream().toList();
	}
}
