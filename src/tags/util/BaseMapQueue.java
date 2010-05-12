// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Collections;

import java.util.Iterator;
import java.util.Comparator;
import java.util.Map;
import java.util.Queue;
import java.util.AbstractQueue;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
** A basic implementation of {@link MapQueue} that uses both a {@link Map} and
** a {@link Queue} to keep track of entries and their ordering.
**
** This implementation will be thread-safe if the {@code sync} argument to the
** constructor is {@code true}.
**
** TODO LOW have another parameter that makes add() and offer() throw an
** exception when a duplicate key is used, instead of re-ordering it.
**
** @param <K> Type of key, and element of queue
** @param <V> Type of value
*/
public class BaseMapQueue<K, V>
extends AbstractQueue<K>
implements MapQueue<K, V> {

	/** Holds key-value mappings. */
	final protected Map<K, V> map;
	/** Value comparator for the queue. */
	final protected Comparator<V> cmp;
	/** Holds keys in order sorted by their values. */
	final protected Queue<K> queue;

	final protected boolean useDefault;
	final protected V dval;

	/**
	** Creates a new queue from the given parameters.
	**
	** @param cmp Comparator for ordering the values
	** @param sync Whether to use {@link PriorityBlockingQueue} ({@code true})
	**        or {@link PriorityQueue} ({@code false}) for the backing queue.
	** @param useDefault Whether to use a default value for {@link #add(Object)}
	** @param dval The default value; only considered if {@code useDefault} is
	**        {@code true}.
	*/
	public BaseMapQueue(Comparator<V> cmp, boolean sync, boolean useDefault, V dval) {
		this.map = sync? new ConcurrentHashMap<K, V>(): new HashMap<K, V>();
		this.cmp = cmp;
		Comparator<K> keycmp = (cmp == null)?
		new Comparator<K>() {
			@SuppressWarnings("unchecked")
			@Override public int compare(K k0, K k1) {
				V v0 = BaseMapQueue.this.map.get(k0), v1 = BaseMapQueue.this.map.get(k1);
				if (v0 == null || v1 == null) { throw new IllegalArgumentException("non-comparable value"); }
				return ((Comparable<V>)v0).compareTo(v1);
			}
		}:
		new Comparator<K>() {
			@Override public int compare(K k0, K k1) {
				return BaseMapQueue.this.cmp.compare(BaseMapQueue.this.map.get(k0), BaseMapQueue.this.map.get(k1));
			}
		};
		this.queue = sync? new PriorityBlockingQueue<K>(11, keycmp): new PriorityQueue<K>(11, keycmp);
		this.useDefault = useDefault;
		this.dval = dval;
	}

	/**
	** Creates a new queue from the given parameters, with no default values
	** (so that {@link #add(Object)} always throws an exception).
	*/
	public BaseMapQueue(Comparator<V> cmp, boolean sync) {
		this(cmp, sync, false, null);
	}

	// MapQueue

	/**
	** {@inheritDoc}
	**
	** This implementation returns an immutable map view.
	*/
	@Override public Map<K, V> map() {
		return Collections.unmodifiableMap(map);
	}

	/**
	** {@inheritDoc}
	**
	** This implementation re-orders existing keys by the new value.
	*/
	@Override public boolean add(K key, V val) {
		if (offer(key, val)) { return true; }
		throw new IllegalStateException("could not add item to queue");
	}

	/**
	** {@inheritDoc}
	**
	** This implementation re-orders existing keys by the new value.
	*/
	@Override public synchronized boolean offer(K key, V val) {
		if (map.containsKey(key)) {
			queue.remove(key);
			map.put(key, val);
			boolean added = queue.offer(key);
			assert added;
			return added;
		} else {
			map.put(key, val);
			if (queue.offer(key)) {
				return true;
			} else {
				map.remove(key);
				return false;
			}
		}
	}

	@Override public synchronized boolean addAll(Map<? extends K, ? extends V> map) {
		if (map.isEmpty()) { return false; }
		for (Map.Entry<? extends K, ? extends V> en: map.entrySet()) {
			add(en.getKey(), en.getValue());
		}
		return true;
	}

	@Override public synchronized V peekValue() {
		// FIXME HIGH NullPointerException occurred here from map.get, probably race condition
		return map.get(queue.peek());
	}

	// Queue

	@Override public boolean offer(K key) {
		if (useDefault) {
			return offer(key, dval);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Override public K peek() {
		return queue.peek();
	}

	@Override public synchronized K poll() {
		K key = queue.poll();
		map.remove(key);
		return key;
	}

	// Collection

	@Override public int size() {
		return queue.size();
	}

	@Override public boolean isEmpty() {
		return queue.isEmpty();
	}

	@Override public synchronized void clear() {
		queue.clear();
		map.clear();
	}

	@Override public boolean contains(Object o) {
		return map.containsKey(o);
	}

	@Override public synchronized boolean remove(Object o) {
		if (map.containsKey(o)) {
			queue.remove(o);
			map.remove(o);
			return true;
		}
		return false;
	}

	@Override public Iterator<K> iterator() {
		return new Iterator<K>() {
			final Iterator<K> it = queue.iterator();
			K last = null;
			@Override public boolean hasNext() { return it.hasNext(); }
			@Override public K next() { return last = it.next(); }
			@Override public void remove() {
				synchronized (BaseMapQueue.this) {
					it.remove();
					assert last != null;
					map.remove(last);
				}
			}
		};
	}

}
