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

	public static Probability ofEntropy(Entropy e) {
		return new Probability(Math.pow(2, -e.val));
	}

}
