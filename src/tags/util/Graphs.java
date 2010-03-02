// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import tags.util.Maps.U2Map;
import tags.util.Union.U2;
import tags.util.Tuple.X2; // TODO NORM move TGraph.Neighbour here
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractSet;
import java.util.AbstractMap;

/**
** Utilities for Graphs (eg. {@link TGraph}, {@link Index}).
*/
final public class Graphs {

	private Graphs() { }

	public static <S, T0, T1, W> void populateOutgoing(U2Map<Arc<S, T0>, Arc<S, T1>, W> arcmap, Map<S, U2Map<T0, T1, W>> outgoing) {
		throw new UnsupportedOperationException("not implemented");
	}

	public static <S, T0, T1, W> void populateIncoming(U2Map<Arc<S, T0>, Arc<S, T1>, W> arcmap, U2Map<T0, T1, Map<S, W>> incoming) {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Returns an immutable (arc-map view) of the given (map of nodes to their
	** outgoing neighbours and arc-attributes).
	**
	** This implementation is efficient for entrySet().iterator(), get(), and
	** containsKey(); and O(V) (ie. better than O(E)) for size(), isEmpty().
	**
	** TODO NORM make the view mutable
	*/
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
