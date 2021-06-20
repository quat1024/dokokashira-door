package agency.highlysuspect.dokokashiradoor.util;

import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Predicate;

public interface RandomSelectable<T> {
	/**
	 * Choose a random element from the collection. This is failable for performance reasons.
	 */
	@Nullable T pickRandom(Random random);
	
	/**
	 * Choose a random element from the collection. If the test passes, remove and return it.
	 */
	@Nullable T removeRandomIf(Random random, Predicate<T> test);
}
