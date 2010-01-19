// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

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
	** @param src_score Map of data sources to their scores, where each data
	**        source is a map from items to values
	** @param item The item to generate a combined value for
	*/
	public <K> V composeValue(Map<Map<K, V>, S> src_score, K item);

}
