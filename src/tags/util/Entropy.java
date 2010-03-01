// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

/**
** Immutable wrapper around a {@code double}, representing an entropy in bits.
*/
public class Entropy /*extends Number TODO LOW*/ implements Comparable<Entropy> {

	/**
	** Minimum possible entropy. Corresponds to {@link Probability#MAX_VALUE}.
	*/
	final public static Entropy MIN_VALUE = new Entropy(0);

	/**
	** Maximum possible entropy. Corresponds to {@link Probability#MIN_VALUE}.
	*/
	final public static Entropy MAX_VALUE = new Entropy(Double.POSITIVE_INFINITY);

	final public double val;

	public Entropy(double v) {
		if (!(0 <= v)) { // catch NaN
			throw new IllegalArgumentException("Invalid entropy");
		}
		// this turns -0.0 to +0.0, so that +0.0 is the unique "minimum element"
		val = (v == 0)? +0.0: v;
	}

	final public static double LOG2 = Math.log(2);

	public Probability probability() {
		return new Probability(Math.pow(2, -val));
	}

	/**
	** Returns the intersection of this and the given entropy. It is assumed
	** that the two events (for which these are entropies) are independent.
	*/
	public Entropy intersect(Entropy e) {
		return new Entropy(val + e.val);
	}

	@Override public int compareTo(Entropy c) {
		return Double.compare(val, c.val);
	}

	@Override public boolean equals(Object o) {
		if (o == this) { return true; }
		if (!(o instanceof Entropy)) { return false; }
		return val == ((Entropy)o).val;
	}

	@Override public int hashCode() {
		long v = Double.doubleToLongBits(val);
		return 1 - (int)(v^(v>>>32)); // 1 - Double.hashCode()
	}

}
