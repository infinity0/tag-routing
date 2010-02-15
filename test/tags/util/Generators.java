// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import tags.util.Tuple.X2;
import static tags.util.Tuple.X2;
import java.util.*;

final public class Generators {

	private Generators() { }

	private static Random rnd = new Random();

	public static String rndStr() {
		return UUID.randomUUID().toString();
	}

	public static String rndKey() {
		return rndStr().substring(0,8);
	}

	public static Integer rndInt() {
		return rnd.nextInt();
	}

	public static Probability rndProb() {
		return new Probability(rnd.nextDouble());
	}

	public static Probability rndProb(double lo) {
		return rndProb(lo, 1.0);
	}

	public static Probability rndProb(double lo, double hi) {
		double val = rnd.nextDouble()*(hi-lo) + lo;
		assert lo <= val && val <= hi;
		return new Probability(val);
	}

	public static Entropy rndEntr() {
		return (new Probability(rnd.nextDouble())).entropy();
	}

	/**
	** Returns an iterator over distinct pairs of the given two lists.
	*/
	public static <T0, T1> Iterator<X2<T0, T1>> rndPairs(final List<T0> c0, final List<T1> c1) {
		return new Iterator<X2<T0, T1>>() {

			final int s0 = c0.size();
			final int s1 = c1.size();
			final Iterator<Integer> it = rndOrdering(s0*s1).iterator();

			@Override public boolean hasNext() {
				return it.hasNext();
			}

			@Override public X2<T0, T1> next() {
				int i = it.next();
				int i0 = i / s1;
				int i1 = i % s1;
				return X2(c0.get(i0), c1.get(i1));
			}

			@Override public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public static List<Integer> rndOrdering(int sz) {
		List<Integer> order = new ArrayList<Integer>();
		for (int i=0; i<sz; ++i) { order.add(i); }
		Collections.shuffle(order);
		return order;
	}

}
