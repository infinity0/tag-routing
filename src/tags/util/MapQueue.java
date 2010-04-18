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
	** View the key-value associations as a {@link Map}. Attempts to write to
	** this map '''must''' "fall through" to the queue, or else throw {@link
	** UnsupportedOperationException}.
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
	** Insert the given key in the same manner as {@link Queue#add(Object)},
	** ordered according to the given value.
	**
	** If the given key already exists, implementations may either throw an
	** exception, or re-order the key according to the new value.
	**
	** @return {@code true} as per {@link Queue#add(Object)}
	*/
	public boolean add(K key, V val);

	/**
	** Insert the given key in the same manner as {@link Queue#offer(Object)},
	** ordered according to the given value.
	**
	** If the given key already exists, implementations may either throw an
	** exception, or re-order the key according to the new value.
	*/
	public boolean offer(K key, V val);

	/**
	** Insert all keys in the same manner as {@link #add(Object, Object)}. If
	** any single insert attempt throws an exception, this may result in only
	** some of the elements being added to (or reordered in) the queue.
	**
	** @return {@code true} if the queue changed (ie. map was not empty)
	*/
	public boolean addAll(Map<? extends K, ? extends V> map);

	/**
	** Peek at the value of the minimum element, returning {@code null} if the
	** queue is empty.
	*/
	public V peekValue();

}
