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
abstract public class MeanProbabilityComposer<R, L, K> implements ValueComposer<R, L, Probability, K, Probability> {

	final protected MapViewer<? super L, ? extends Map<? extends K, ? extends Probability>> viewer;

	public MeanProbabilityComposer(MapViewer<? super L, ? extends Map<? extends K, ? extends Probability>> viewer) {
		this.viewer = viewer;
	}

	/**
	** {@inheritDoc}
	**
	** DOCUMENT the precise formula used here...
	**
	** @throws NullPointerException if any of the data sources map the item to
	**         a {@code null} probability
	*/
	@Override public Probability composeValue(MapX2<? extends R, ? extends L, ? extends Probability> source, K item) {
		double top = 0, div = 0;
		for (X2<? extends L, ? extends Probability> x: source.values()) {
			Map<? extends K, ? extends Probability> view = viewer.mapFor(x._0);
			Probability score = x._1;
			if (view.containsKey(item)) {
				top += score.val * view.get(item).val;
				div += score.val;
			} else {
				div += score.val * alpha(x._0, item);
			}
		}
		return new Probability(top/div);
	}

	/**
	** Returns the probability that the the given data source implicitly judges
	** the given item to be worthless.
	*/
	abstract protected double alpha(L view, K item);

}
