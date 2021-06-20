package agency.highlysuspect.dokokashiradoor.util;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class FunnySet<T> extends ObjectOpenHashSet<T> {
	public FunnySet() {
		super();
	}
	
	public FunnySet(Collection<T> t) {
		super(t);
	}
	
	public static <T> Codec<FunnySet<T>> codec(Codec<T> element) {
		return element.listOf().xmap(FunnySet::new, FunnySet::asList);
	}
	
	public List<T> asList() {
		return stream().toList();
	}
}
