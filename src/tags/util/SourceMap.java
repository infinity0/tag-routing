// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.AbstractMap;
import java.util.AbstractSet;

import tags.util.Maps;

import java.util.Iterator;
import java.util.Set;
import java.util.Map;

/**
** A class for viewing (maps of objects) as (maps of maps), suitable to use as
** input into a {@link ValueComposer}. DOCUMENT explain better...
**
** Note that the performance of lookup and retrieval methods are crap, because
** they use the default implementations of {@link AbstractMap}. The only
** "efficient" methods here are {@link #entrySet()} and {@link #size()}.
**
** @param <T> Type of object to view as a map
** @param <S> Type of score (weight of a data source)
** @param <K> Type of item
** @param <V> Type of value (weight of a data item)
*/
abstract public class SourceMap<T, S, K, V> extends AbstractMap<Map<K, V>, S> implements Map<Map<K, V>, S> {

	final public Map<T, S> smap;

	public SourceMap(Map<T, S> smap) {
		this.smap = smap;
	}

	/**
	** Returns a map view of the given object (or part of it).
	*/
	abstract protected Map<K, V> mapFor(T obj);

	@Override public int size() {
		return smap.size();
	}

	@Override public void clear() {
		smap.clear();
	}

	private transient Set<Map.Entry<Map<K, V>, S>> entries;
	@Override public Set<Map.Entry<Map<K, V>, S>> entrySet() {
		if (entries == null) {
			entries = new AbstractSet<Map.Entry<Map<K, V>, S>>() {

				final Iterable<Map.Entry<Map<K, V>, S>> ib = new CompositeIterable<Map.Entry<T, S>, Map.Entry<Map<K, V>, S>>(smap.entrySet()) {
					@Override public Map.Entry<Map<K, V>, S> nextFor(Map.Entry<T, S> en) {
						return Maps.composeEntry(en, mapFor(en.getKey()));
					}
				};

				@Override public Iterator<Map.Entry<Map<K, V>, S>> iterator() {
					return ib.iterator();
				}

				@Override public int size() {
					return SourceMap.this.size();
				}

				@Override public void clear() {
					SourceMap.this.clear();
				}

			};
		}
		return entries;
	}

}
