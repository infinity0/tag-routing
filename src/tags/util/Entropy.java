// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

/**
** Immutable wrapper around a {@code double}, representing an entropy in bits.
*/
public class Entropy /*extends Number TODO LOW*/ {

	final public double val;

	public Entropy(double v) {
		if (v < 0) {
			throw new IllegalArgumentException("Invalid entropy");
		}
		val = v;
	}

	final public static double LOG2 = Math.log(2);

	public Probability probability() {
		return new Probability(Math.pow(2, -val));
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
