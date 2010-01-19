// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

/**
** Immutable wrapper around a {@code double}, representing a probability.
*/
public class Probability /*extends Number TODO LOW*/ {

	final public double val;

	public Probability(double v) {
		if (v < 0 || v > 1) {
			throw new IllegalArgumentException("Invalid probability");
		}
		val = v;
	}

	public Probability complement() {
		return new Probability(1 - val);
	}

	public Entropy entropy() {
		return new Entropy(-Math.log(val)/Entropy.LOG2);
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
