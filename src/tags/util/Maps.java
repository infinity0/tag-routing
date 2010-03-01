// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Arrays;

import tags.util.Union.U2;
import tags.util.Tuple.X2;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractSet;
import java.util.AbstractMap;
import java.util.HashSet;

/**
** Utilities for {@link Map}s.
*/
final public class Maps {

	private Maps() { }

	/**
	** Returns an entry backed by the given entry but with a different key.
	** Updates to the value are reflected in both entries.
	**
	** @param <J> Type of source key
	** @param <K> Type of target key
	** @param <V> Type of value
	*/
	public static <J, K, V> Map.Entry<K, V> composeEntry(final Map.Entry<J, V> en, K key) {
		return new AbstractEntry<K, V>(key) {
			@Override public V getValue() { return en.getValue(); }
			@Override public V setValue(V val) { return en.setValue(val); }
		};
	}

	/**
	** A basic {@link Entry}. DOCUMENT more details.
	*/
	abstract public static class AbstractEntry<K, V> implements Map.Entry<K, V> {

		final public K key;
		public AbstractEntry(K key) { this.key = key; }
		@Override public K getKey() { return key; }

		@Override abstract public V getValue();

		@Override public V setValue(V val) {
			throw new UnsupportedOperationException("setValue() not overridden");
		}

		@Override public boolean equals(Object o) {
			if (o == this) { return true; }
			if (!(o instanceof Map.Entry)) { return false; }
			Map.Entry e2 = (Map.Entry)o;
			return (key == null? e2.getKey() == null: key.equals(e2.getKey()))
			    && (getValue() == null? e2.getValue() == null : getValue().equals(e2.getValue()));
		}

		@Override public int hashCode() {
			return (key == null? 0: key.hashCode()) ^ (getValue() == null? 0: getValue().hashCode());
		}

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
	public static <K, V> Set<K> domain(Iterable<? extends Map<K, V>> maps) {
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
	** Returns a submap view of the {@code parent} map, with at most the given
	** {@code keys}. If any key is not in the parent map, it will not be in the
	** submap. Ie. the view's keyset is always a subset of, or equal to, the
	** given {@code keys}.
	**
	** '''NOTE''': the {@link Map#size()} method has linear time complexity.
	*/
	public static <K, V> Map<K, V> viewSubMap(final Map<K, V> parent, final Set<K> keys) {
		return new AbstractMap<K, V>() {

			// No more efficient way than AbstractMap's default implementation that defers to entrySet().size()
			// @Override public int size() { }

			@Override public boolean containsKey(Object o) {
				return keys.contains(o) && parent.containsKey(o);
			}

			// No more efficient way than AbstractMap's default implementation that searches the entire map
			// @Override public boolean containsValue(Object o) { }

			@Override public V get(Object o) {
				return keys.contains(o)? parent.get(o): null;
			}

			@Override public V put(K key, V val) {
				if (!keys.contains(key)) {
					throw new IllegalArgumentException("inappropriate key for submap: " + key);
				}
				return parent.put(key, val);
			}

			@Override public V remove(Object o) {
				return keys.contains(o)? parent.remove(o): null;
			}

			// No more efficient way than AbstractMap's default implementation that iterates over the map
			// @Override public void putAll(Map<? extends K, ? extends V> map) { }

			@Override public void clear() {
				parent.keySet().removeAll(keys);
			}

			private transient Set<Map.Entry<K, V>> entries;
			@Override public Set<Map.Entry<K, V>> entrySet() {
				if (entries == null) {
					entries = new AbstractSet<Map.Entry<K, V>>() {

						@Override public int size() {
							int i = 0;
							for (Iterator<Map.Entry<K, V>> it = iterator(); it.hasNext(); ++i) { it.next(); }
							return i;
						}

						@Override public boolean contains(Object o) {
							if (!(o instanceof Map.Entry)) { return false; }
							@SuppressWarnings("unchecked") Map.Entry<K, V> en = (Map.Entry<K, V>)o;
							K key = en.getKey();
							V testval = en.getValue();
							if (!keys.contains(key)) { return false; }

							V val = parent.get(key);
							return val == null? parent.containsKey(key) && testval == null: val.equals(testval);
						}

						@Override public boolean remove(Object o) {
							if (!(o instanceof Map.Entry)) { return false; }
							@SuppressWarnings("unchecked") Map.Entry<K, V> en = (Map.Entry<K, V>)o;
							K key = en.getKey();
							V testval = en.getValue();
							if (!keys.contains(key)) { return false; }

							V val = parent.get(key);
							if (val == null? parent.containsKey(key) && testval == null: val.equals(testval)) {
								parent.remove(key);
								return true;
							} else {
								return false;
							}
						}

						@Override public Iterator<Map.Entry<K, V>> iterator() {
							return new Iterator<Map.Entry<K, V>>() {
								Iterator<K> kit = keys.iterator();
								K cur, last;
								boolean hasnext = findNext(), removeok = false;

								private boolean findNext() {
									while (kit.hasNext()) {
										K key = kit.next();
										if (parent.containsKey(key)) {
											cur = key;
											return true;
										}
									}
									return false;
								}

								@Override public boolean hasNext() {
									return hasnext;
								}

								@Override public Map.Entry<K, V> next() {
									if (!hasnext) { throw new java.util.NoSuchElementException(); }
									Map.Entry<K, V> en = new AbstractEntry<K, V>(last = cur) {
										@Override public V getValue() { return parent.get(key); }
										@Override public V setValue(V val) { return parent.put(key, val); }
									};
									removeok = true;
									hasnext = findNext();
									return en;
								}

								@Override public void remove() {
									if (removeok) {
										parent.remove(last);
										removeok = false;
									} else {
										throw new IllegalStateException("not appropriate for remove call");
									}
								}
							};
						}

					};
				}
				return entries;
			}

		};
	}

	/**
	** A {@link Map} which can have two different types of key.
	**
	** @param <K0> Type of key 0
	** @param <K1> Type of key 1
	** @param <V> Type of value
	*/
	public static interface U2Map<K0, K1, V> extends Map<U2<K0, K1>, V> {
		/** Returns a view of the map containing only keys of type {@code K0}. */
		public Map<K0, V> K0Map();
		/** Returns a view of the map containing only keys of type {@code K1}. */
		public Map<K1, V> K1Map();
	}

	/**
	** A {@link Maps.U2Map} which uses a more specific type of component map.
	**
	** @param <K0> Type of key 0
	** @param <K1> Type of key 1
	** @param <V> Type of value
	** @param <M0> Type of map 0
	** @param <M1> Type of map 1
	*/
	public static interface U2MMap<K0, K1, V, M0 extends Map<K0, V>, M1 extends Map<K1, V>> extends U2Map<K0, K1, V> {
		/** {@inheritDoc} */
		@Override public M0 K0Map();
		/** {@inheritDoc} */
		@Override public M1 K1Map();
	}

	/**
	** Returns a view of the disjoint union of two maps.
	**
	** @see Maps.BaseU2Map
	*/
	public static <K0, K1, V, M0 extends Map<K0, V>, M1 extends Map<K1, V>>
	U2MMap<K0, K1, V, M0, M1> uniteDisjoint(M0 m0, M1 m1) {
		return new BaseU2Map<K0, K1, V, M0, M1>(m0, m1);
	}

	/**
	** DOCUMENT.
	**
	** It is '''assumed''' that the two maps are disjoint and will always
	** remain disjoint; it is up to the caller to ensure that this holds.
	**
	** TODO LOW atm this class iterates through m1, then m0. Would be nice to
	** be able to define a custom iteration order through the constructor.
	*/
	public static class BaseU2Map<K0, K1, V, M0 extends Map<K0, V>, M1 extends Map<K1, V>>
	extends AbstractMap<U2<K0, K1>, V>
	implements U2MMap<K0, K1, V, M0, M1> {

		final protected M0 m0;
		final protected M1 m1;

		public BaseU2Map(M0 m0, M1 m1) {
			this.m0 = m0;
			this.m1 = m1;
		}

		@Override public M0 K0Map() { return m0; }
		@Override public M1 K1Map() { return m1; }

		@Override public int size() {
			return m0.size() + m1.size();
		}

		@Override public boolean containsKey(Object o) {
			if (!(o instanceof U2)) { return false; }
			@SuppressWarnings("unchecked") U2<K0, K1> u = (U2<K0, K1>)o;
			return u.type == 0? m0.containsKey(u.getT0()): m1.containsKey(u.getT1());
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
						V testval = en.getValue();

						Object key = u.isT0()? u.getT0(): u.getT1();
						Map<?, V> map = u.isT0()? m0: m1;
						V val = map.get(key);
						return val == null? map.containsKey(key) && testval == null: val.equals(testval);
					}

					@Override public boolean remove(Object o) {
						if (!(o instanceof Map.Entry)) { return false; }
						@SuppressWarnings("unchecked") Map.Entry<U2<K0, K1>, V> en = (Map.Entry<U2<K0, K1>, V>)o;
						U2<K0, K1> u = en.getKey();
						V testval = en.getValue();

						Object key = u.isT0()? u.getT0(): u.getT1();
						Map<?, V> map = u.isT0()? m0: m1;
						V val = map.get(key);

						if (val == null? map.containsKey(key) && testval == null: val.equals(testval)) {
							map.remove(key);
							return true;
						} else {
							return false;
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

	/**
	** A {@link Map} which has two values for each key.
	**
	** @param <K> Type of key
	** @param <V0> Type of value 0
	** @param <V1> Type of value 1
	*/
	public static interface MapX2<K, V0, V1> extends Map<K, X2<V0, V1>> {
		/** Returns a view of the map containing only values of type {@code V0}. */
		public Map<K, V0> MapV0();
		/** Returns a view of the map containing only values of type {@code V1}. */
		public Map<K, V1> MapV1();
	}

	/**
	** A {@link Maps.MapX2} which uses a more specific type of component map.
	**
	** @param <K> Type of key
	** @param <V0> Type of value 0
	** @param <V1> Type of value 1
	** @param <M0> Type of map 0
	** @param <M1> Type of map 1
	*/
	public static interface MMapX2<K, V0, V1, M0 extends Map<K, V0>, M1 extends Map<K, V1>> extends MapX2<K, V0, V1> {
		/** {@inheritDoc} */
		@Override public M0 MapV0();
		/** {@inheritDoc} */
		@Override public M1 MapV1();
	}

	/**
	** Returns a view of the strict convolution of two maps. Only keys in both
	** maps will be present in the view.
	**
	** @see Maps.BaseMapX2
	*/
	public static <K, V0, V1, M0 extends Map<K, V0>, M1 extends Map<K, V1>>
	MMapX2<K, V0, V1, M0, M1> convoluteStrict(M0 m0, M1 m1, BaseMapX2.Inclusion inc) {
		return new BaseMapX2<K, V0, V1, M0, M1>(m0, m1, inc);
	}

	/**
	** DOCUMENT.
	*/
	public static class BaseMapX2<K, V0, V1, M0 extends Map<K, V0>, M1 extends Map<K, V1>>
	extends AbstractMap<K, X2<V0, V1>>
	implements MMapX2<K, V0, V1, M0, M1> {

		public enum Inclusion {
			/** Both maps will always have the same keys */
			EQUAL,
			/** {@link #m0} will always be a subseteq of {@link #m1} */
			SUB0SUP1,
			/** {@link #m1} will always be a subseteq of {@link #m0} */
			SUB1SUP0
		}

		final Inclusion inc;
		final protected M0 m0;
		final protected M1 m1;

		public BaseMapX2(M0 m0, M1 m1, Inclusion inc) {
			this.m0 = m0;
			this.m1 = m1;
			this.inc = inc;
		}

		public BaseMapX2(M0 m0, M1 m1) {
			this(m0, m1, Inclusion.EQUAL);
		}

		@Override public M0 MapV0() { return m0; }
		@Override public M1 MapV1() { return m1; }

		@Override public int size() {
			int s0 = m0.size(), s1 = m1.size();
			switch (inc) {
			case EQUAL: assert s0 == s1; return s0;
			case SUB0SUP1: assert s0 <= s1; return s0;
			case SUB1SUP0: assert s0 >= s1; return s1;
			}
			throw new AssertionError();
		}

		@Override public boolean containsKey(Object o) {
			switch (inc) {
			case EQUAL: return m0.containsKey(o) && m1.containsKey(o);
			case SUB0SUP1: return m0.containsKey(o);
			case SUB1SUP0: return m1.containsKey(o);
			}
			throw new AssertionError();
		}

		// No more efficient way than AbstractMap's default implementation that searches the entire map
		// @Override public boolean containsValue(Object o) { }

		@Override public X2<V0, V1> get(Object o) {
			return containsKey(o)? Tuple.X2(m0.get(o), m1.get(o)): null;
		}

		/**
		** {@inheritDoc}
		**
		** This will add {@code key} to both maps, even if one of the values
		** of the tuple is {@code null}.
		*/
		@Override public X2<V0, V1> put(K key, X2<V0, V1> x) {
			if (containsKey(key)) {
				return Tuple.X2(m0.put(key, x._0), m1.put(key, x._1));
			} else {
				m0.put(key, x._0);
				m1.put(key, x._1);
				return null;
			}
		}

		@Override public X2<V0, V1> remove(Object o) {
			if (containsKey(o)) {
				return Tuple.X2(m0.remove(o), m1.remove(o));
			} else {
				m0.remove(o);
				m1.remove(o);
				return null;
			}
		}

		// No more efficient way than AbstractMap's default implementation which iterates over the map
		// @Override public void putAll(Map<? extends K, ? extends X2<V0, V1>> map) { }

		@Override public void clear() {
			m0.clear();
			m1.clear();
		}

		private transient Set<Map.Entry<K, X2<V0, V1>>> entries;
		@Override public Set<Map.Entry<K, X2<V0, V1>>> entrySet() {
			if (entries == null) {
				entries = new AbstractSet<Map.Entry<K, X2<V0, V1>>>() {

					@SuppressWarnings("unchecked")
					final Iterable<Map.Entry<K, X2<V0, V1>>> ib = buildIterable();
					private Iterable<Map.Entry<K, X2<V0, V1>>> buildIterable() {
						switch (inc) {
						case EQUAL:
						case SUB0SUP1:
							return new CompositeIterable<Map.Entry<K, V0>, Map.Entry<K, X2<V0, V1>>>(m0.entrySet(), CompositeIterable.Mutability.REMOVE_CLEANUP) {
								@Override public void removeFor(Map.Entry<K, V0> en) { m1.remove(en.getKey()); }
								@Override public Map.Entry<K, X2<V0, V1>> nextFor(final Map.Entry<K, V0> en) {
									return new AbstractEntry<K, X2<V0, V1>>(en.getKey()) {
										@Override public X2<V0, V1> getValue() {
											return Tuple.X2(en.getValue(), m1.get(key));
										}
										@Override public X2<V0, V1> setValue(X2<V0, V1> x) {
											return Tuple.X2(en.setValue(x._0), m1.put(key, x._1));
										}
									};
								}
							};
						case SUB1SUP0:
							return new CompositeIterable<Map.Entry<K, V1>, Map.Entry<K, X2<V0, V1>>>(m1.entrySet(), CompositeIterable.Mutability.REMOVE_CLEANUP) {
								@Override public void removeFor(Map.Entry<K, V1> en) { m0.remove(en.getKey()); }
								@Override public Map.Entry<K, X2<V0, V1>> nextFor(final Map.Entry<K, V1> en) {
									return new AbstractEntry<K, X2<V0, V1>>(en.getKey()) {
										@Override public X2<V0, V1> getValue() {
											return Tuple.X2(m0.get(key), en.getValue());
										}
										@Override public X2<V0, V1> setValue(X2<V0, V1> x) {
											return Tuple.X2(m0.put(key, x._0), en.setValue(x._1));
										}
									};
								}
							};
						}
						throw new AssertionError();
					}

					@Override public int size() { return BaseMapX2.this.size(); }

					@Override public void clear() { BaseMapX2.this.clear(); }

					@Override public boolean contains(Object o) {
						if (!(o instanceof Map.Entry)) { return false; }
						@SuppressWarnings("unchecked") Map.Entry<K, X2<V0, V1>> en = (Map.Entry<K, X2<V0, V1>>)o;
						K key = en.getKey();
						X2<V0, V1> u = en.getValue();

						V0 v0 = m0.get(key);
						V1 v1 = m1.get(key);
						return (v0 == null? m0.containsKey(key) && u._0 == null: v0.equals(u._0))
						    && (v1 == null? m1.containsKey(key) && u._1 == null: v1.equals(u._1));
					}

					@Override public boolean remove(Object o) {
						if (!(o instanceof Map.Entry)) { return false; }
						@SuppressWarnings("unchecked") Map.Entry<K, X2<V0, V1>> en = (Map.Entry<K, X2<V0, V1>>)o;
						K key = en.getKey();
						X2<V0, V1> u = en.getValue();

						V0 v0 = m0.get(key);
						V1 v1 = m1.get(key);

						if ((v0 == null? m0.containsKey(key) && u._0 == null: v0.equals(u._0))
						 && (v1 == null? m1.containsKey(key) && u._1 == null: v1.equals(u._1))) {
							m0.remove(key);
							m1.remove(key);
							return true;
						} else {
							return false;
						}
					}

					@Override public Iterator<Map.Entry<K, X2<V0, V1>>> iterator() {
						return ib.iterator();
					}

				};
			}
			return entries;
		}

	}

	public static <K0, K1, V0, V1> U2MapX2<K0, K1, V0, V1> convoluteStrictUniteDisjoint(
		Map<K0, V0> m00, Map<K1, V0> m10,
		Map<K0, V1> m01, Map<K1, V1> m11,
		BaseMapX2.Inclusion inc
	) {
		return new BaseU2MapX2<K0, K1, V0, V1>(m00, m10, m01, m11, inc);
	}

	/**
	** DOCUMENT.
	**
	** TODO LOW possibly make this also {@code extend U2Map<K0, K1, X2<V0, V1>, MapX2<K0, V0, V1>, MapX2<K1, V0, V1>>}
	*/
	public static interface U2MapX2<K0, K1, V0, V1> extends MMapX2<U2<K0, K1>, V0, V1, U2Map<K0, K1, V0>, U2Map<K0, K1, V1>> {
		// for convience, like "typedef"
	}

	public static class BaseU2MapX2<K0, K1, V0, V1>
	extends BaseMapX2<U2<K0, K1>, V0, V1, U2Map<K0, K1, V0>, U2Map<K0, K1, V1>>
	implements U2MapX2<K0, K1, V0, V1> {

		@SuppressWarnings("unchecked") // unnecessary unchecked casts below are WORKAROUND for incomplete java type inference
		public BaseU2MapX2(Map<K0, V0> m00, Map<K1, V0> m10, Map<K0, V1> m01, Map<K1, V1> m11, BaseMapX2.Inclusion inc) {
			super((U2Map<K0, K1, V0>)uniteDisjoint(m00, m10), (U2Map<K0, K1, V1>)uniteDisjoint(m01, m11), inc);
		}

	}


	public static <S, T0, T1, W> U2Map<Arc<S, T0>, Arc<S, T1>, W> viewAsArcMap(final Map<S, U2Map<T0, T1, W>> outgoing) {
		return Maps.uniteDisjoint(
			new AbstractMap<Arc<S, T0>, W>() {

				@Override public boolean containsKey(Object o) {
					if (!(o instanceof Arc)) { return false; }
					@SuppressWarnings("unchecked") Arc<S, T0> arc = (Arc<S, T0>)o;
					return outgoing.containsKey(arc.src) && outgoing.get(arc.src).K0Map().containsKey(arc.dst);
				}

				@Override public W get(Object o) {
					if (!(o instanceof Arc)) { return null; }
					@SuppressWarnings("unchecked") Arc<S, T0> arc = (Arc<S, T0>)o;
					return outgoing.containsKey(arc.src)? outgoing.get(arc.src).K0Map().get(arc.dst): null;
				}

				private transient Set<Map.Entry<Arc<S, T0>, W>> entries;
				@Override public Set<Map.Entry<Arc<S, T0>, W>> entrySet() {
					if (entries == null) {
						entries = new AbstractSet<Map.Entry<Arc<S, T0>, W>>() {

							@SuppressWarnings("unchecked")
							final Iterable<Map.Entry<Arc<S, T0>, W>> ib = new ChainIterable<Map.Entry<Arc<S, T0>, W>>(true,
								new CompositeIterable<Map.Entry<S, U2Map<T0, T1, W>>, Iterable<Map.Entry<Arc<S, T0>, W>>>(outgoing.entrySet()) {
									@Override public Iterable<Map.Entry<Arc<S, T0>, W>> nextFor(final Map.Entry<S, U2Map<T0, T1, W>> enu) {
										return new CompositeIterable<Map.Entry<T0, W>, Map.Entry<Arc<S, T0>, W>>(enu.getValue().K0Map().entrySet()) {
											@Override public Map.Entry<Arc<S, T0>, W> nextFor(Map.Entry<T0, W> en) {
												return Maps.composeEntry(en, Arc.arc(enu.getKey(), en.getKey()));
											}
										};
									}
								}
							);

							@Override public int size() {
								int s = 0;
								for (U2Map<T0, T1, W> u2m: outgoing.values()) {
									s += u2m.K0Map().size();
								}
								return s;
							}

							@Override public boolean contains(Object o) {
								if (!(o instanceof Map.Entry)) { return false; }
								@SuppressWarnings("unchecked") Map.Entry<Arc<S, T0>, W> en = (Map.Entry<Arc<S, T0>, W>)o;
								Arc<S, T0> key = en.getKey();
								W testval = en.getValue();

								U2Map<T0, T1, W> out = outgoing.get(key.src);
								if (out == null) { return false; }
								W val = out.K0Map().get(key.dst);
								return val == null? out.K0Map().containsKey(key.dst) && testval == null: val.equals(testval);
							}

							/*@Override public boolean remove(Object o) {
								if (!(o instanceof Map.Entry)) { return false; }
								@SuppressWarnings("unchecked") Map.Entry<Arc<S, T0>, W> en = (Map.Entry<Arc<S, T0>, W>)o;
								Arc<S, T0> key = en.getKey();
								W testval = en.getValue();

								U2Map<T0, T1, W> out = outgoing.get(key.src);
								if (out == null) { return false; }
								W val = out.K0Map().get(key.dst);
								if (val == null? out.K0Map().containsKey(key.dst) && testval == null: val.equals(testval)) {
									out.K0Map().remove(key.dst);
									if (out.isEmpty()) { outgoing.remove(key.src); }
									return true;
								} else {
									return false;
								}
							}*/

							@Override public Iterator<Map.Entry<Arc<S, T0>, W>> iterator() {
								return ib.iterator();
							}

						};
					}
					return entries;
				}

			},

			new AbstractMap<Arc<S, T1>, W>() {

				@Override public boolean containsKey(Object o) {
					if (!(o instanceof Arc)) { return false; }
					@SuppressWarnings("unchecked") Arc<S, T1> arc = (Arc<S, T1>)o;
					return outgoing.containsKey(arc.src) && outgoing.get(arc.src).K1Map().containsKey(arc.dst);
				}

				@Override public W get(Object o) {
					if (!(o instanceof Arc)) { return null; }
					@SuppressWarnings("unchecked") Arc<S, T1> arc = (Arc<S, T1>)o;
					return outgoing.containsKey(arc.src)? outgoing.get(arc.src).K1Map().get(arc.dst): null;
				}

				private transient Set<Map.Entry<Arc<S, T1>, W>> entries;
				@Override public Set<Map.Entry<Arc<S, T1>, W>> entrySet() {
					if (entries == null) {
						entries = new AbstractSet<Map.Entry<Arc<S, T1>, W>>() {

							@SuppressWarnings("unchecked")
							final Iterable<Map.Entry<Arc<S, T1>, W>> ib = new ChainIterable<Map.Entry<Arc<S, T1>, W>>(true,
								new CompositeIterable<Map.Entry<S, U2Map<T0, T1, W>>, Iterable<Map.Entry<Arc<S, T1>, W>>>(outgoing.entrySet()) {
									@Override public Iterable<Map.Entry<Arc<S, T1>, W>> nextFor(final Map.Entry<S, U2Map<T0, T1, W>> enu) {
										return new CompositeIterable<Map.Entry<T1, W>, Map.Entry<Arc<S, T1>, W>>(enu.getValue().K1Map().entrySet()) {
											@Override public Map.Entry<Arc<S, T1>, W> nextFor(Map.Entry<T1, W> en) {
												return Maps.composeEntry(en, Arc.arc(enu.getKey(), en.getKey()));
											}
										};
									}
								}
							);

							@Override public int size() {
								int s = 0;
								for (U2Map<T0, T1, W> u2m: outgoing.values()) {
									s += u2m.K1Map().size();
								}
								return s;
							}

							@Override public boolean contains(Object o) {
								if (!(o instanceof Map.Entry)) { return false; }
								@SuppressWarnings("unchecked") Map.Entry<Arc<S, T1>, W> en = (Map.Entry<Arc<S, T1>, W>)o;
								Arc<S, T1> key = en.getKey();
								W testval = en.getValue();

								U2Map<T0, T1, W> out = outgoing.get(key.src);
								if (out == null) { return false; }
								W val = out.K1Map().get(key.dst);
								return val == null? out.K1Map().containsKey(key.dst) && testval == null: val.equals(testval);
							}

							/*@Override public boolean remove(Object o) {
								if (!(o instanceof Map.Entry)) { return false; }
								@SuppressWarnings("unchecked") Map.Entry<Arc<S, T1>, W> en = (Map.Entry<Arc<S, T1>, W>)o;
								Arc<S, T1> key = en.getKey();
								W testval = en.getValue();

								U2Map<T0, T1, W> out = outgoing.get(key.src);
								if (out == null) { return false; }
								W val = out.K1Map().get(key.dst);
								if (val == null? out.K1Map().containsKey(key.dst) && testval == null: val.equals(testval)) {
									out.K1Map().remove(key.dst);
									if (out.isEmpty()) { outgoing.remove(key.src); }
									return true;
								} else {
									return false;
								}
							}*/

							@Override public Iterator<Map.Entry<Arc<S, T1>, W>> iterator() {
								return ib.iterator();
							}

						};
					}
					return entries;
				}

			}

		);

	}

}
