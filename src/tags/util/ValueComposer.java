// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import tags.util.Maps.MapX2;
import java.util.Map;

/**
** An object for composing value judgements from multiple data sources.
**
** @param <S> Type of score (weight of a data source)
** @param <V> Type of value (weight of a data item)
*/
public interface ValueComposer<S, V> {

	/**
	** Returns the combined value of an item, given a set of weighted data
	** sources, each possibly holding value judgements for the item.
	**
	** @param source Map of data sources to their local views and scores
	** @param item The item to generate a combined value for
	** @param <R> Type of remote address
	** @param <K> Type of item
	*/
	public <R, L, K> V composeValue(MapX2<R, L, S> source, K item, MapViewer<L, Map<K, V>> viewer);

}
