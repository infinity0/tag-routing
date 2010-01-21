// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.util.Probability;
import tags.util.Entropy;

import java.util.Comparator;

/**
** DOCUMENT.
*/
public class EntropyDistanceMetric implements DistanceMetric<Entropy, Probability, Probability> {

	@Override public Entropy getDistance(Probability srcw, Probability dstw, Probability arcw) {
		return dstw.intersect(arcw).entropy();
	}

	@Override public Entropy getMinElement() {
		return Entropy.MIN_VALUE;
	}

	@Override public Entropy getMaxElement() {
		return Entropy.MAX_VALUE;
	}

	@Override public Entropy combine(Entropy d1, Entropy d2) {
		return d1.intersect(d2);
	}

	@Override public int compare(Entropy d1, Entropy d2) {
		return Double.compare(d1.val, d2.val);
	}

}
