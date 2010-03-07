// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Map;
import java.util.Queue;

/**
** A {@link Queue} with its elements ordered according to their associated
** values in a map. (Duplicate elements are not allowed; attempts to add an
** element twice will overwrite the first add.)
**
** @param <K> Type of key, and element of queue
** @param <V> Type of value
*/
public interface MapQueue<K, V> extends Queue<K> {

	/**
	** View the key-value associations as a {@link Map}. Writes should "fall
	** through" to the queue, or throw {@link UnsupportedOperationException}.
	*/
	public Map<K, V> map();

	/**
	** {@inheritDoc}
	**
	** Implementations may insert the key at an arbitrary point in the queue,
	** or throw {@link UnsupportedOperationException}.
	**
	** @see Queue#add(Object)
	*/
	@Override public boolean add(K key);

	/**
	** {@inheritDoc}
	**
	** Implementations may insert the key at an arbitrary point in the queue,
	** or throw {@link UnsupportedOperationException}.
	**
	** @see Queue#offer(Object)
	*/
	@Override public boolean offer(K key);

	/**
	** Insert the given key according to {@link Queue#add(Object)}, ordered
	** according to the given value.
	*/
	public boolean add(K key, V val);

	/**
	** Insert the given key according to {@link Queue#offer(Object)}, ordered
	** according to the given value.
	*/
	public boolean offer(K key, V val);

}
