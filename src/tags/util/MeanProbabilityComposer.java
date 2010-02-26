// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import tags.util.Maps.MapX2;
import tags.util.Tuple.X2;
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
	@Override public <R, L, K> Probability composeValue(MapX2<R, L, Probability> source, K item, MapViewer<L, ? extends Map<K, Probability>> viewer) {
		double top = 0, div = 0;
		for (X2<L, Probability> x: source.values()) {
			Map<K, Probability> view = viewer.mapFor(x._0);
			Probability score = x._1;
			if (view.containsKey(item)) {
				top += score.val * view.get(item).val;
				div += score.val;
			} else {
				div += score.val * alpha(view, item);
			}
		}
		return new Probability(top/div);
	}

	/**
	** Returns the probability that the the given data source implicitly judges
	** the given item to be worthless.
	*/
	abstract protected <K> double alpha(Map<K, Probability> view, K item);

}
