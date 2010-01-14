// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

/**
** Immutable wrapper around a {@link double}, representing an entropy in bits.
*/
public class Entropy /*extends Number*/ {

	final public double val;

	public Entropy(double v) {
		if (v < 0) {
			throw new IllegalArgumentException("Invalid entropy");
		}
		val = v;
	}

	final public static double LOG2 = Math.log(2);

	public static Entropy ofProbability(Probability p) {
		return new Entropy(-Math.log(p.val)/LOG2);
	}

}
