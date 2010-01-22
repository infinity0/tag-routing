// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

/**
** Immutable wrapper around a {@code double}, representing a probability.
*/
public class Probability /*extends Number TODO LOW*/ implements Comparable<Probability> {

	/**
	** Minimum possible probability. Corresponds to {@link Entropy#MAX_VALUE}.
	*/
	final public static Probability MIN_VALUE = new Probability(0);

	/**
	** Maximum possible probability. Corresponds to {@link Entropy#MIN_VALUE}.
	*/
	final public static Probability MAX_VALUE = new Probability(1);

	final public double val;

	public Probability(double v) {
		if (v < 0 || v > 1) {
			throw new IllegalArgumentException("Invalid probability");
		}
		// this turns -0.0 to +0.0, so that +0.0 is the unique "minimum element"
		val = (v == 0)? +0.0: v;
	}

	public Probability complement() {
		return new Probability(1 - val);
	}

	public Entropy entropy() {
		return new Entropy(-Math.log(val)/Entropy.LOG2);
	}

	/**
	** Returns the intersection of this and the given probability. It is assumed
	** that the two events (for which these are probabilities) are independent.
	*/
	public Probability intersect(Probability p) {
		return new Probability(val * p.val);
	}

	@Override public int compareTo(Probability c) {
		return Double.compare(val, c.val);
	}

	@Override public boolean equals(Object o) {
		if (o == this) { return true; }
		if (!(o instanceof Probability)) { return false; }
		return val == ((Probability)o).val;
	}

	@Override public int hashCode() {
		long v = Double.doubleToLongBits(val);
		return 1 + (int)(v^(v>>>32)); // 1 + Double.hashCode()
	}

}
