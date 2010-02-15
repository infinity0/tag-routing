// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.*;

final public class Generators {

	private Generators() { }

	private static Random rnd = new Random();

	public static Probability randomProbability() {
		return new Probability(rnd.nextDouble());
	}

	public static Probability randomProbability(double lo) {
		return randomProbability(lo, 1.0);
	}

	public static Probability randomProbability(double lo, double hi) {
		double val = rnd.nextDouble()*(hi-lo) + lo;
		assert lo <= val && val <= hi;
		return new Probability(val);
	}

	public static Entropy randomEntropy() {
		return (new Probability(rnd.nextDouble())).entropy();
	}

}
