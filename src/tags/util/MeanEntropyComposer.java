// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import tags.util.Maps.MapX2;
import tags.util.Tuple.X2;
import java.util.Map;

/**
** DOCUMENT. more details...
**
** TODO NORM maybe somehow merge this with MeanProbabilityComposer
*/
abstract public class MeanEntropyComposer<R, L, K> implements ValueComposer<R, L, Probability, K, Entropy> {

	final protected MapViewer<? super L, ? extends Map<? extends K, ? extends Entropy>> viewer;

	public MeanEntropyComposer(MapViewer<? super L, ? extends Map<? extends K, ? extends Entropy>> viewer) {
		if (viewer == null) { throw new NullPointerException(); }
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
	@Override public Entropy composeValue(MapX2<? extends R, ? extends L, ? extends Probability> source, K item) {
		double top = 0, div = 0;
		for (X2<? extends L, ? extends Probability> x: source.values()) {
			Map<? extends K, ? extends Entropy> view = viewer.mapFor(x._0);
			Probability score = x._1;
			if (view.containsKey(item)) {
				top += score.val * view.get(item).val;
				div += score.val;
			} else {
				div += score.val * alpha(x._0, item);
			}
		}
		return new Entropy(top/div);
	}

	/**
	** Returns the probability that the the given data source implicitly judges
	** the given item to be worthless.
	*/
	abstract protected double alpha(L view, K item);

}
