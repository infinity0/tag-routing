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

	/**
	** {@inheritDoc}
	**
	** This implementation takes {@code idxs} to be P(index), and {@code tagw}
	** to be P(seed|subj), and returns P(index) P(seed|subj), which is supposed
	** to be an approximation of P(seed, subj, index).
	**
	** # P(seed, subj, index)
	** # = P(seed | index, subj) P(index|subj) [''chain rule'']
	** # ≅ P(seed|subj) P(index) [''assume independence'']
	**
	** @see tags.proto.Notation
	*/
	public Probability getLookupScore(Probability idxs, Probability tagw) {
		return idxs.intersect(tagw);
	}

	/**
	** {@inheritDoc}
	**
	** This implementation takes {@code tagw} to be P(seed|subj), and {@code
	** docw} to be P(doc ∊ subj), and returns P(seed|subj) P(doc ∊ subj), which
	** is supposed to be P(doc ∊ seed, doc ∊ subj).
	**
	** # P(doc ∊ seed, doc ∊ subj)
	** # = P(doc ∊ seed | doc ∊ subj) P(doc ∊ subj) [''chain rule'']
	** # = P(seed|subj) P(doc ∊ subj)
	**
	** TODO HIGH we should actually return P(doc ∊ seed) according to the below
	** formula, but then we will need extra inputs to this method.
	**
	** # P(doc ∊ seed)
	** # = P(doc ∊ seed, doc ∊ subj) + P(doc ∊ seed, doc ∊ ¬subj)
	** # = P(seed|subj) P(doc ∊ subj) + P(seed|¬subj) (1-P(doc ∊ subj))
	**
	** @param tagw P(seed|subj)
	** @param docw P(doc ∊ subj)
	** @see tags.proto.Notation
	*/
	public Probability getResultAttr(Probability tagw, Probability docw) {
		// return tagw.intersect(docw).union(seedu.complementConditional(tagw, tagu), docw.complement());
		return tagw.intersect(docw);
	}

}
