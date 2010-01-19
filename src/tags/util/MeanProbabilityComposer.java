// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Map;

/**
** Composes probabilities by computing the weighted average of all judgements,
** attempting to infer between zero-knowledge and active rejection.
**
** DOCUMENT. more details...
*/
abstract public class MeanProbabilityComposer implements ValueComposer<Probability, Probability> {

	/**
	** {@inheritDoc}
	**
	** @throws NullPointerException if any of the data sources map the item to
	**         a {@code null} probability
	*/
	public <K> Probability composeValue(Map<Map<K, Probability>, Probability> src_score, K item) {
		double top = 0, div = 0;
		for (Map.Entry<Map<K, Probability>, Probability> en: src_score.entrySet()) {
			Map<K, Probability> src = en.getKey();
			Probability score = en.getValue();
			if (src.containsKey(item)) {
				top += score.val * src.get(item).val;
				div += score.val;
			} else {
				div += score.val * alpha(src, item);
			}
		}
		return new Probability(top/div);
	}

	/**
	** Returns the probability that the the given data source implicitly judges
	** the given item to be worthless.
	*/
	abstract protected <K> double alpha(Map<K, Probability> src, K item);

}
