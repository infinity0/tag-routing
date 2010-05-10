// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.AddressScheme;
import tags.proto.LocalIndex;
import tags.proto.Index.Lookup;
import tags.util.Probability;
import java.util.Comparator;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;

/**
** DOCUMENT.
*/
public class ProbabilityLookupScorer implements LookupScorer<Probability, Probability> {

	/**
	** {@inheritDoc}
	**
	** TODO HIGH flawed; we should use P(subj, index) not P(index).
	** entire process needs to be re-thought
	**
	** @see tags.proto.Notation
	*/
	public Probability getLookupScore(Probability idxs, Probability tagw) {
		return idxs.intersect(tagw);
	}

	public Probability getPotential(Collection<Probability> lkuw) {
		// FIXME HIGH this is a hack...
		//return Collections.max(lkuw);
		Probability m = Collections.max(lkuw);
		return Probability.unionInd(Arrays.asList(m, m, m, m));
		//return new Probability(Probability.unionInd(lkuw).val / 4);
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
	** TODO NORM we should actually return P(doc ∊ seed) according to the below
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
