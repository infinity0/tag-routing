// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.AddressScheme;
import tags.proto.LocalIndex;
import tags.proto.Lookup;
import tags.util.Probability;
import java.util.Comparator;
import java.util.Set;
import java.util.Map;
import java.util.PriorityQueue;

/**
** DOCUMENT.
**
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public class ProbabilityLookupScorer implements LookupScorer<Probability, Probability> {

	public <T, A> PriorityQueue<Lookup<T, A>> sortLookups(final Map<Lookup<T, A>, Probability> lookups) {
		PriorityQueue<Lookup<T, A>> queue = new PriorityQueue<Lookup<T, A>>(lookups.size(),
			new Comparator<Lookup<T, A>>() {
				@Override public int compare(Lookup<T, A> l0, Lookup<T, A> l1) {
					Probability p0 = lookups.get(l0);
					Probability p1 = lookups.get(l1);
					if (p0 == null || p1 == null) {
						throw new IllegalArgumentException("unscored lookup");
					}
					// we want highest element first
					return p1.compareTo(p0);
				};
			}
		);
		queue.addAll(lookups.keySet());
		return queue;
	}

	public Probability getLookupScore(Probability idxs, Probability tagw) {
		return idxs.intersect(tagw);
	}

	public Probability getResultAttr(Probability tagw, Probability docw) {
		return tagw.intersect(docw);
	}

}
