// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.AbstractSet;
import java.util.AbstractMap;

/**
** A proxy to a backing {@link Map}. The {@link #itemFor(Object)} method must
** be overridden to convert source items to target items.
**
** To create a mutable map, the {@link #inverseItemFor(Object)} method may be
** overridden to convert target items to source items. (To create an immutable
** map, simply do not override this method.)
**
** @param <K> Type of key
** @param <S> Type of source value
** @param <T> Type of target value
*/
abstract public class ProxyMap<K, S, T> extends AbstractMap<K, T> implements Map<K, T> {

	final protected Map<K, S> map;

	public ProxyMap(Map<K, S> map) {
		this.map = map;
	}

	abstract protected T itemFor(S elem);

	protected S inverseItemFor(T elem) {
		throw new UnsupportedOperationException("inverse not defined");
	}

	@Override public int size() {
		return map.size();
	}

	@Override public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override public void clear() {
		map.clear();
	}

	@Override public boolean containsKey(Object o) {
		return map.containsKey(o);
	}

	@SuppressWarnings("unchecked")
	@Override public boolean containsValue(Object o) {
		// OPT NORM if it throws once then don't bother trying another time
		try {
			return map.containsValue(inverseItemFor((T)o));
		} catch (UnsupportedOperationException e) {
			return super.containsValue(o);
		}
	}

	@Override public T get(Object o) {
		return itemFor(map.get(o));
	}

	@Override public T remove(Object o) {
		return itemFor(map.remove(o));
	}

	@Override public T put(K key, T val) {
		return itemFor(map.put(key, inverseItemFor(val)));
	}

	private transient Set<Map.Entry<K, T>> entries;
	@Override public Set<Map.Entry<K, T>> entrySet() {
		if (entries == null) {
			entries = new AbstractSet<Map.Entry<K, T>>() {

				@SuppressWarnings("unchecked")
				final Iterable<Map.Entry<K, T>> ib = new ProxyIterable<Map.Entry<K, S>, Map.Entry<K, T>>(map.entrySet()) {
					@Override public Map.Entry<K, T> nextFor(final Map.Entry<K, S> en) {
						return new Maps.AbstractEntry<K, T>(en.getKey()) {
							@Override public T getValue() { return itemFor(en.getValue()); }
							@Override public T setValue(T val) { return itemFor(en.setValue(inverseItemFor(val))); }
						};
					}
				};

				@Override public int size() {
					return map.size();
				}

				@Override public boolean contains(Object o) {
					if (!(o instanceof Map.Entry)) { return false; }
					@SuppressWarnings("unchecked") Map.Entry<K, T> en = (Map.Entry<K, T>)o;
					K key = en.getKey();
					T testval = en.getValue();

					S val = map.get(key);
					return val == null? testval == null && map.containsKey(key): itemFor(val).equals(testval);
				}

				@Override public boolean remove(Object o) {
					if (!(o instanceof Map.Entry)) { return false; }
					@SuppressWarnings("unchecked") Map.Entry<K, T> en = (Map.Entry<K, T>)o;
					K key = en.getKey();
					T testval = en.getValue();

					S val = map.get(key);
					if (val == null? testval == null && map.containsKey(key): itemFor(val).equals(testval)) {
						map.remove(key);
						return true;
					} else {
						return false;
					}
				}

				@Override public Iterator<Map.Entry<K, T>> iterator() {
					return ib.iterator();
				}

			};
		}
		return entries;
	}

}
