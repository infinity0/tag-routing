// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import tags.proto.TGraph;
import tags.proto.Index;
import tags.util.Maps.U2Map;
import tags.util.Union.U2;
import tags.util.Tuple.X2; // TODO NORM move TGraph.Neighbour here
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractSet;
import java.util.AbstractMap;
import java.util.HashMap;

/**
** Utilities for Graphs (eg. {@link TGraph}, {@link Index}).
*/
final public class Graphs {

	private Graphs() { }

	/**
	** Populate the given empty data structures from the given node-set and
	** arc-map. The arc-map must refer only to nodes in the set.
	**
	** TODO LOW make this use a Factory instead of just constructing HashMap
	**
	** @param node_src Set of source nodes
	** @param node_dst_0 Set of target (0) nodes
	** @param node_dst_1 Set of target (1) nodes
	** @param arc_map Map of arcs to their attributes
	** @param outgoing [Output] Map of nodes to their outgoing neighbours
	** @param incoming [Output] Map of nodes to their incoming neighbours
	** @param <S> Type of source node
	** @param <T0> Type of target node (0)
	** @param <T1> Type of target node (1)
	** @param <W> Type of arc attribute
	** @throws NullPointerException if any argument except {@code incoming} is
	**         {@code null}.
	** @throws IllegalArgumentException if the output structures are not empty,
	**         or if the arc-map refers to objects not contained in the node
	**         sets.
	*/
	public static <S, T0, T1, W> void populateFromNodesAndArcs(
		Set<S> node_src,
		Set<T0> node_dst_0,
		Set<T1> node_dst_1,
		U2Map<Arc<S, T0>, Arc<S, T1>, W> arc_map,
		Map<S, U2Map<T0, T1, W>> outgoing,
		U2Map<T0, T1, Map<S, W>> incoming
	) {
		if (!outgoing.isEmpty()) { throw new IllegalArgumentException("outgoing not empty"); }
		if (incoming != null && !incoming.isEmpty()) { throw new IllegalArgumentException("incoming not empty"); }

		// init arc holders
		for (S src: node_src) {
			U2Map<T0, T1, W> u2m = Maps.uniteDisjoint(new HashMap<T0, W>(), new HashMap<T1, W>());
			outgoing.put(src, u2m);
		}
		if (incoming != null) {
			for (T0 dst: node_dst_0) { incoming.K0Map().put(dst, new HashMap<S, W>()); }
			for (T1 dst: node_dst_1) { incoming.K1Map().put(dst, new HashMap<S, W>()); }
		}

		for (Map.Entry<Arc<S, T0>, W> en: arc_map.K0Map().entrySet()) {
			Arc<S, T0> arc = en.getKey(); W attr = en.getValue();

			U2Map<T0, T1, W> out = outgoing.get(arc.src);
			if (out == null) { throw new IllegalArgumentException("arc " + arc + " source not defined"); }
			out.K0Map().put(arc.dst, attr);

			if (incoming == null) {
				if (!node_dst_0.contains(arc.dst)) {
					throw new IllegalArgumentException("arc " + arc + " target not defined");
				}
				continue;
			}

			Map<S, W> in = incoming.K0Map().get(arc.dst);
			if (in == null) { throw new IllegalArgumentException("arc " + arc + " target not defined"); }
			in.put(arc.src, attr);
		}

		for (Map.Entry<Arc<S, T1>, W> en: arc_map.K1Map().entrySet()) {
			Arc<S, T1> arc = en.getKey(); W attr = en.getValue();

			U2Map<T0, T1, W> out = outgoing.get(arc.src);
			if (out == null) { throw new IllegalArgumentException("arc " + arc + " source not defined"); }
			out.K1Map().put(arc.dst, attr);

			if (incoming == null) {
				if (!node_dst_1.contains(arc.dst)) {
					throw new IllegalArgumentException("arc " + arc + " target not defined");
				}
				continue;
			}

			Map<S, W> in = incoming.K1Map().get(arc.dst);
			if (in == null) { throw new IllegalArgumentException("arc " + arc + " target not defined"); }
			in.put(arc.src, attr);
		}
	}

	/**
	** Populate the given empty data structures from the given arc-map. The
	** node-sets will be filled automatically.
	**
	** TODO LOW make this use a Factory instead of just constructing HashMap
	**
	** @param arc_map Map of arcs to their attributes
	** @param outgoing [Output] Map of nodes to their outgoing neighbours
	** @param incoming [Output] Map of nodes to their incoming neighbours
	** @param node_src [Output] Set of source nodes
	** @param node_dst_0 [Output] Set of target (0) nodes
	** @param node_dst_1 [Output] Set of target (1) nodes
	** @param <S> Type of source node
	** @param <T0> Type of target node (0)
	** @param <T1> Type of target node (1)
	** @param <W> Type of arc attribute
	** @throws NullPointerException if any argument except {@code incoming} is
	**         {@code null}.
	** @throws IllegalArgumentException if the output structures are not empty
	*/
	public static <S, T0, T1, W> void populateFromArcMap(
		U2Map<Arc<S, T0>, Arc<S, T1>, W> arc_map,
		Map<S, U2Map<T0, T1, W>> outgoing,
		U2Map<T0, T1, Map<S, W>> incoming,
		Set<S> node_src,
		Set<T0> node_dst_0,
		Set<T1> node_dst_1
	) {
		if (!node_src.isEmpty()) { throw new IllegalArgumentException("node_src not empty"); }
		if (!node_dst_0.isEmpty()) { throw new IllegalArgumentException("node_dst_0 not empty"); }
		if (!node_dst_1.isEmpty()) { throw new IllegalArgumentException("node_dst_1 not empty"); }
		if (!outgoing.isEmpty()) { throw new IllegalArgumentException("outgoing not empty"); }
		if (incoming != null && !incoming.isEmpty()) { throw new IllegalArgumentException("incoming not empty"); }

		for (Map.Entry<Arc<S, T0>, W> en: arc_map.K0Map().entrySet()) {
			Arc<S, T0> arc = en.getKey(); W attr = en.getValue();
			node_src.add(arc.src); node_dst_0.add(arc.dst);

			U2Map<T0, T1, W> out = outgoing.get(arc.src);
			if (out == null) { outgoing.put(arc.src, out = Maps.uniteDisjoint(new HashMap<T0, W>(), new HashMap<T1, W>())); }
			out.K0Map().put(arc.dst, attr);

			if (incoming == null) { continue; }

			Map<S, W> in = incoming.K0Map().get(arc.dst);
			if (in == null) { incoming.K0Map().put(arc.dst, in = new HashMap<S, W>()); }
			in.put(arc.src, attr);
		}

		for (Map.Entry<Arc<S, T1>, W> en: arc_map.K1Map().entrySet()) {
			Arc<S, T1> arc = en.getKey(); W attr = en.getValue();
			node_src.add(arc.src); node_dst_1.add(arc.dst);

			U2Map<T0, T1, W> out = outgoing.get(arc.src);
			if (out == null) { outgoing.put(arc.src, out = Maps.uniteDisjoint(new HashMap<T0, W>(), new HashMap<T1, W>())); }
			out.K1Map().put(arc.dst, attr);

			if (incoming == null) { continue; }

			Map<S, W> in = incoming.K1Map().get(arc.dst);
			if (in == null) { incoming.K1Map().put(arc.dst, in = new HashMap<S, W>()); }
			in.put(arc.src, attr);
		}
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
