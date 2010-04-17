// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.util.Probability;
import java.util.Comparator;

/**
** DOCUMENT.
**
** This implementation uses the distance metric D[n] = P(t[1], ..., t[n] | t[0]),
** with D[0] = 1, which has the property that D[n+1] = D[n] * P(t[n+1] | t[n]),
** assuming all adjacent pairs are independent.
**
** Proof: This holds for n=0, by inspection. Then by induction:
**
** # D[n+1]
** # = k * P(t[0], ..., t[n], t[n+1]) [k = 1 / P(t[0])]
** # = k * P(t[0] | t[1]) * ... * P(t[n-1] | t[n]) * P(t[n] | t[n+1]) * P(t[n+1]) [''assume independence'']
** # = k * P(t[0], ..., t[n]) / P(t[n]) * P(t[n] | t[n+1]) * P(t[n+1])
** # = k * P(t[0], ..., t[n]) * P(t[n+1] | t[n])
** # = D[n] * P(t[n+1] | t[n])
**
** Therefore this holds for all natural integers n.
**
** Whether adjacent pairs are actually independent is left up to the class that
** uses this metric. In particular, {@link ShortestPathAddressSchemeBuilder}
** does '''not''' guarantee that this holds.
*/
public class ProbabilityDistanceMetric implements DistanceMetric<Probability, Probability, Probability> {

	@Override public Probability identity() {
		return Probability.MAX_VALUE;
	}

	@Override public Probability infinity() {
		return Probability.MIN_VALUE;
	}

	/**
	** {@inheritDoc}
	**
	** @param srcu P(src)
	** @param dstu P(dst)
	** @param arcw P(src|dst)
	** @return P(dst|src)
	**
	** @see tags.proto.Notation
	*/
	@Override public Probability getDistance(Probability srcu, Probability dstu, Probability arcw) {
		try {
			return srcu.conditionalInverse(arcw, dstu);
		} catch (IllegalArgumentException e) {
			System.err.println("tried to calculate P(t|s) from P(s)=" + srcu.val + " P(t)=" + dstu.val + " P(s|t)=" + arcw.val);
			//throw e;
			return new Probability(1.0);
		}
	}

	/**
	** {@inheritDoc}
	**
	** @param d0 P(evt0)
	** @param d1 P(evt1)
	** @return P(evt0)*P(evt1)
	*/
	@Override public Probability combine(Probability d0, Probability d1) {
		return d0.intersect(d1);
	}

	/**
	** {@inheritDoc}
	**
	** @param seedu P(seed)
	** @param subju P(subj)
	** @param dist P(etc, subj | seed)
	** @return P(etc, seed | subj)
	*/
	@Override public Probability getAttrFromDistance(Probability seedu, Probability subju, Probability dist) {
		return subju.conditionalInverse(dist, seedu);
	}

	@Override public int compare(Probability d0, Probability d1) {
		return Double.compare(d1.val, d0.val); // higher probability is "nearer"
	}

}
