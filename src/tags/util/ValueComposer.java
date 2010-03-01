// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import tags.util.Maps.MapX2;
import java.util.Map;

/**
** An object for composing value judgements from multiple data sources.
**
** @param <R> Type of remote address
** @param <L> Type of local view
** @param <S> Type of score (weight of a data source)
** @param <V> Type of value (weight of a data item)
** @param <K> Type of data item (key)
*/
public interface ValueComposer<R, L, S, K, V> {

	/**
	** Returns the combined value of an item, given a set of weighted data
	** sources, each possibly holding value judgements for the item.
	**
	** @param source Map of data sources to their local views and scores
	** @param item The item to generate a combined value for
	*/
	public V composeValue(MapX2<? extends R, ? extends L, ? extends S> source, K item);

}
