// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import junit.framework.TestCase;

import java.util.*;

public class ProbabilityEntropyTest extends TestCase {

	public void testValidValues() {
		assertTrue((new Probability(0.0)).equals(new Probability(-0.0)));
		assertTrue((new Probability(0.5)).equals(new Probability(0.5)));
		assertTrue((new Probability(1.0)).equals(new Probability(1.0)));

		checkProbabilityArgument(-0.1);
		checkProbabilityArgument(1.1);
		checkProbabilityArgument(Double.NaN);
		checkProbabilityArgument(Double.POSITIVE_INFINITY);
		checkProbabilityArgument(Double.NEGATIVE_INFINITY);

		assertTrue((new Entropy(0.0)).equals(new Entropy(-0.0)));
		assertTrue((new Entropy(1.0)).equals(new Entropy(1.0)));
		assertTrue((new Entropy(Double.POSITIVE_INFINITY)).equals(new Entropy(Double.POSITIVE_INFINITY)));

		checkEntropyArgument(-0.1);
		checkEntropyArgument(Double.NaN);
		checkEntropyArgument(Double.NEGATIVE_INFINITY);
	}

	public void testConversion() {
		assertTrue(Probability.MIN_VALUE.entropy().equals(Entropy.MAX_VALUE));
		assertTrue(Probability.MAX_VALUE.entropy().equals(Entropy.MIN_VALUE));
		assertTrue(Entropy.MIN_VALUE.probability().equals(Probability.MAX_VALUE));
		assertTrue(Entropy.MAX_VALUE.probability().equals(Probability.MIN_VALUE));
	}

	public void testProbabilityMethods() {
		Probability s = new Probability(0.5);
		Probability s_ = new Probability(0.5);
		Probability t = new Probability(0.75);
		Probability t_ = new Probability(0.25);
		Probability st = new Probability(0.46875);

		assertTrue(s.union(s_).equals(Probability.MAX_VALUE));
		assertTrue(t.union(t_).equals(Probability.MAX_VALUE));

		Probability s_given_t = st.normalise(t);
		assertTrue(s_given_t.equals(new Probability(0.625)));
		assertTrue(s.conditionalInverse(s_given_t, t).equals(st.normalise(s)));
		assertTrue(s.conditionalComplement(s_given_t, t).equals(new Probability(0.125)));
	}

	public static void checkProbabilityArgument(double d) {
		try {
			new Probability(d); fail();
		} catch (RuntimeException e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
	}

	public static void checkEntropyArgument(double d) {
		try {
			new Entropy(d); fail();
		} catch (RuntimeException e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
	}

}
