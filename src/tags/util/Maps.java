// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Arrays;

import tags.util.Union.U2;
import tags.util.Tuple.X2;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.AbstractSet;
import java.util.AbstractMap;
import java.util.HashSet;

/**
** Utilities for {@link Map}s.
*/
final public class Maps {

	private Maps() { }

	/**
	** Return an entry backed by the given entry but with a different key.
	** Updates to the value are reflected in both entries.
	**
	** @param <J> Type of source key
	** @param <K> Type of target key
	** @param <V> Type of value
	*/
	public static <J, K, V> Map.Entry<K, V> composeEntry(final Map.Entry<J, V> en, final K key) {
		return new Map.Entry<K, V>() {
			@Override public K getKey() { return key; }
			@Override public V getValue() { return en.getValue(); }
			@Override public V setValue(V val) { return en.setValue(val); }
			@Override public boolean equals(Object o) {
				if (o == this) { return true; }
				if (!(o instanceof Map.Entry)) { return false; }
				Map.Entry e2 = (Map.Entry)o;
				return (key == null? e2.getKey() == null: key.equals(e2.getKey())) && (en.getValue() == null? e2.getValue() == null : en.getValue().equals(e2.getValue()));
			}
			@Override public int hashCode() {
				return (key == null? 0: key.hashCode()) ^ (en.getValue() == null? 0: en.getValue().hashCode());
			}
		};
	}

	/**
	** @see #domain(Iterable)
	*/
	public static <K, V> Set<K> domain(Map<K, V>... maps) {
		return domain(Arrays.asList(maps));
	}

	/**
	** Returns the union of all the domains of the given collection of maps.
	**
	** Currently, this does '''not''' return a view of the given maps, and will
	** '''not''' appear to self-update when the maps change.
	**
	** OPT LOW making it a view seems quite complicated...
	*/
	public static <K, V> Set<K> domain(Iterable<Map<K, V>> maps) {
		int s = 0;
		for (Map<K, V> m: maps) { s += m.size(); }
		Set<K> domain = new HashSet<K>(s<<1);
		for (Map<K, V> m: maps) { domain.addAll(m.keySet()); }
		return domain;
	}

	/**
	** Returns the nodes which the given arcmap refers to. DOCUMENT explain
	** better.
	**
	** Currently, this does '''not''' return a view of the given map, and will
	** '''not''' appear to self-update when the map changes.
	**
	** OPT LOW making it a view seems quite complicated...
	*/
	public static <K extends Arc, V> Set<Object> referent(Map<K, V> arcmap) {
		Set<Object> referent = new HashSet<Object>(arcmap.size()<<1);
		for (K arc: arcmap.keySet()) {
			referent.add(arc.src);
			referent.add(arc.dst);
		}
		return referent;
	}

	/**
	** A {@link Map} which can have two different types of key.
	**
	** @param <K0> Type of key 0
	** @param <K1> Type of key 1
	** @param <V> Type of value
	*/
	public static interface U2Map<K0, K1, V> extends Map<U2<K0, K1>, V> {

		/**
		** Return a view of the map containing only keys of type {@code K0}.
		*/
		public Map<K0, V> K0Map();

		/**
		** Return a view of the map containing only keys of type {@code K1}.
		*/
		public Map<K1, V> K1Map();

	}

	/**
	** A {@link Map} which has two values for each key.
	**
	** @param <K> Type of key
	** @param <V0> Type of value 0
	** @param <V1> Type of value 1
	*/
	public static interface MapX2<K, V0, V1, M0 extends Map<K, V0>, M1 extends Map<K, V1>> extends Map<K, X2<V0, V1>> {

		/**
		** Return a view of the map containing only values of type {@code V0}.
		*/
		public M0 MapV0();

		/**
		** Return a view of the map containing only values of type {@code V1}.
		*/
		public M1 MapV1();

	}

	/**
	** DOCUMENT.
	**
	** TODO LOW possibly make this also {@code extend U2Map<K0, K1, X2<V0, V1>, MapX2<K0, V0, V1>, MapX2<K1, V0, V1>>}
	*/
	public static interface U2MapX2<K0, K1, V0, V1> extends MapX2<U2<K0, K1>, V0, V1, U2Map<K0, K1, V0>, U2Map<K0, K1, V1>> {
		// for convience, like "typedef"
	}

	/**
	** Returns a view of the disjoint union of two maps.
	**
	** It is '''assumed''' that the two maps are disjoint and will always
	** remain disjoint; it is up to the caller to ensure that this holds.
	**
	** @see Maps.BaseU2Map
	*/
	public static <K0, K1, V> U2Map<K0, K1, V> unionDisjoint(Map<K0, V> m0, Map<K1, V> m1) {
		return new BaseU2Map<K0, K1, V>(m0, m1);
	}

	/**
	** DOCUMENT.
	**
	** TODO LOW atm this class iterates through m1, then m0. Would be nice to
	** be able to define a custom iteration order through the constructor.
	*/
	public static class BaseU2Map<K0, K1, V> extends AbstractMap<U2<K0, K1>, V> implements U2Map<K0, K1, V> {

		final protected Map<K0, V> m0;
		final protected Map<K1, V> m1;

		public BaseU2Map(Map<K0, V> m0, Map<K1, V> m1) {
			this.m0 = m0;
			this.m1 = m1;
		}

		@Override public Map<K0, V> K0Map() { return m0; }
		@Override public Map<K1, V> K1Map() { return m1; }

		@Override public int size() {
			return m0.size() + m1.size();
		}

		@Override public boolean containsKey(Object o) {
			if (!(o instanceof U2)) { return false; }
			@SuppressWarnings("unchecked") U2<K0, K1> u = (U2<K0, K1>)o;
			return (u == null)? false: (u.type == 0)? m0.containsKey(u.getT0()): m1.containsKey(u.getT1());
		}

		@Override public boolean containsValue(Object o) {
			return m0.containsValue(o) || m1.containsValue(o);
		}

		@Override public V get(Object o) {
			if (!(o instanceof U2)) { return null; }
			@SuppressWarnings("unchecked") U2<K0, K1> u = (U2<K0, K1>)o;
			return (u.type == 0)? m0.get(u.getT0()): m1.get(u.getT1());
		}

		@Override public V put(U2<K0, K1> u, V val) {
			return (u.type == 0)? m0.put(u.getT0(), val): m1.put(u.getT1(), val);
		}

		@Override public V remove(Object o) {
			if (!(o instanceof U2)) { return null; }
			@SuppressWarnings("unchecked") U2<K0, K1> u = (U2<K0, K1>)o;
			return (u == null)? null: (u.type == 0)? m0.remove(u.getT0()): m1.remove(u.getT1());
		}

		@Override public void putAll(Map<? extends U2<K0, K1>, ? extends V> map) {
			if (!(map instanceof U2Map)) { super.putAll(map); return; }
			@SuppressWarnings("unchecked") U2Map<K0, K1, V> umap = (U2Map<K0, K1, V>)(U2Map)map;
			m0.putAll(umap.K0Map());
			m1.putAll(umap.K1Map());
		}

		@Override public void clear() {
			m0.clear();
			m1.clear();
		}

		private transient Set<Map.Entry<U2<K0, K1>, V>> entries;
		@Override public Set<Map.Entry<U2<K0, K1>, V>> entrySet() {
			if (entries == null) {
				entries = new AbstractSet<Map.Entry<U2<K0, K1>, V>>() {

					@SuppressWarnings("unchecked")
					final Iterable<Map.Entry<U2<K0, K1>, V>> ib = new ChainIterable<Map.Entry<U2<K0, K1>, V>>(
						false, (Iterable<Map.Entry<U2<K0, K1>, V>>[]) new Iterable[] {
							new CompositeIterable<Map.Entry<K1, V>, Map.Entry<U2<K0, K1>, V>>(m1.entrySet()) {
								@Override public Map.Entry<U2<K0, K1>, V> nextFor(final Map.Entry<K1, V> en) {
									return Maps.composeEntry(en, Union.<K0, K1>U2_1(en.getKey()));
								}
							},
							new CompositeIterable<Map.Entry<K0, V>, Map.Entry<U2<K0, K1>, V>>(m0.entrySet()) {
								@Override public Map.Entry<U2<K0, K1>, V> nextFor(final Map.Entry<K0, V> en) {
									return Maps.composeEntry(en, Union.<K0, K1>U2_0(en.getKey()));
								}
							}
						}
					);

					@Override public int size() { return BaseU2Map.this.size(); }

					@Override public void clear() { BaseU2Map.this.clear(); }

					@Override public boolean contains(Object o) {
						if (!(o instanceof Map.Entry)) { return false; }
						@SuppressWarnings("unchecked") Map.Entry<U2<K0, K1>, V> en = (Map.Entry<U2<K0, K1>, V>)o;
						U2<K0, K1> u = en.getKey();
						V val, testval = en.getValue();
						if (u.type == 0) {
							if (!m0.containsKey(u.getT0())) { return false; }
							val = m0.get(u.getT0());
						} else {
							if (!m1.containsKey(u.getT1())) { return false; }
							val = m1.get(u.getT1());
						}
						return val == null && testval == null || val.equals(testval);
					}

					@Override public boolean remove(Object o) {
						if (!(o instanceof Map.Entry)) { return false; }
						@SuppressWarnings("unchecked") Map.Entry<U2<K0, K1>, V> en = (Map.Entry<U2<K0, K1>, V>)o;
						U2<K0, K1> u = en.getKey();
						V testval = en.getValue();
						if (u.type == 0) {
							if (!m0.containsKey(u.getT0())) { return false; }
							V val = m0.get(u.getT0());
							if (val == null && testval == null || val.equals(testval)) {
								m0.remove(u.getT0());
								return true;
							} else {
								return false;
							}
						} else {
							if (!m1.containsKey(u.getT1())) { return false; }
							V val = m1.get(u.getT1());
							if (val == null && testval == null || val.equals(testval)) {
								m0.remove(u.getT0());
								return true;
							} else {
								return false;
							}
						}
					}

					@Override public Iterator<Map.Entry<U2<K0, K1>, V>> iterator() {
						return ib.iterator();
					}

				};
			}
			return entries;
		}

	}

}
