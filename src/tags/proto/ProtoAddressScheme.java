// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Maps;
import tags.util.Union;
import java.util.Collections;
import java.util.Arrays;

import tags.util.Union.U2;
import tags.util.Maps.U2Map;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <W> Type of arc-attribute
*/
public class ProtoAddressScheme<T, A, W> implements AddressScheme<T, A, W> {

	final protected List<U2<T, A>> node_list = new ArrayList<U2<T, A>>();
	final protected U2Map<T, A, Integer> node_map = Maps.uniteDisjoint(new HashMap<T, Integer>(), new HashMap<A, Integer>());

	final protected Map<T, Set<U2<T, A>>> outgoing = new HashMap<T, Set<U2<T, A>>>();
	final protected U2Map<T, A, Set<T>> incoming = Maps.uniteDisjoint(new HashMap<T, Set<T>>(), new HashMap<A, Set<T>>());
	final protected U2Map<T, A, Set<T>> ancestor = Maps.uniteDisjoint(new HashMap<T, Set<T>>(), new HashMap<A, Set<T>>());
	final protected U2Map<T, A, List<T>> path = Maps.uniteDisjoint(new HashMap<T, List<T>>(), new HashMap<A, List<T>>());

	final protected Map<T, W> arc_attr_map = new HashMap<T, W>();
	final protected Comparator<W> cmp;

	protected boolean incomplete = false;
	protected T incomplete_tag;
	protected A nearest_tgraph;

	/**
	** Construct a new scheme with the given tag as the zeroth (ie. seed) tag.
	** If {@code cmp} is used, then {@link #getMostRelevant(Set)} will use the
	** natural ordering for {@code <W>} (it must be {@link Comparable}).
	*/
	public ProtoAddressScheme(T src, Comparator<W> cmp) {
		this.cmp = cmp;
		// initialise structures with src tag
		node_list.add(Union.<T, A>U2_0(src));
		node_map.K0Map().put(src, 0);
		outgoing.put(src, new HashSet<U2<T, A>>());
		incoming.K0Map().put(src, Collections.<T>emptySet());
		ancestor.K0Map().put(src, Collections.<T>emptySet());
		path.K0Map().put(src, Collections.<T>singletonList(src));
	}

	@Override public T seedTag() {
		U2<T, A> zero = node_list.get(0);
		return zero.getT0();
	}

	@Override public Set<T> tagSet() {
		return Collections.unmodifiableSet(arc_attr_map.keySet());
	}

	@Override public Map<T, W> arcAttrMap() {
		return Collections.unmodifiableMap(arc_attr_map);
	}

	@Override public List<U2<T, A>> nodeList() {
		return Collections.unmodifiableList(node_list);
	}

	@Override public U2Map<T, A, Integer> indexMap() {
		// FIXME NORM should really be immutable view
		return node_map;
	}

	@Override public U2Map<T, A, List<T>> pathMap() {
		// FIXME NORM should really be immutable view
		return path;
	}

	@Override public U2Map<T, A, Set<T>> ancestorMap() {
		// FIXME NORM should really be immutable view
		return ancestor;
	}

	@Override public boolean isIncomplete() {
		return incomplete;
	}

	@Override public T getIncomplete() {
		return incomplete_tag;
	}

	@Override public Comparator<W> comparator() {
		return cmp;
	}

	@SuppressWarnings("unchecked")
	@Override public int compare(W w0, W w1) {
		return cmp == null? ((Comparable<W>)w0).compareTo(w1): cmp.compare(w0, w1);
	}

	@Override public <K> Map.Entry<K, W> getMostRelevant(Map<K, W> map) {
		if (map.isEmpty()) { return null; }
		return Maps.<K, W>immutableEntry(Collections.max(map.entrySet(), Maps.<K, W>entryValueComparator(cmp)));
	}

	@Override public Map.Entry<T, W> getMostRelevant(Set<T> tags) {
		return getMostRelevant(Maps.viewSubMap(arc_attr_map, tags));
	}

	@Override public A getNearestTGraph() {
		return nearest_tgraph;
	}

	/**
	** @throws IllegalStateException if the last node to be push is not a tag
	*/
	public void setIncomplete() {
		U2<T, A> node = node_list.get(node_list.size()-1);
		if (node.isT0()) {
			incomplete_tag = node.getT0();
			incomplete = true;
		} else {
			throw new IllegalStateException("last node is not a tag");
		}
	}

	/**
	** Attaches the given node to the address scheme, with a set of incoming
	** neighbours. Only nodes already in the scheme (ie. nearer to the seed)
	** will be added as incoming neighbours; the rest will be filtered out.
	**
	** It is '''assumed''' that nodes are added in shortest-path order. It is
	** up to the caller to ensure that this holds.
	**
	** @param node The node to push onto this address scheme
	** @param parent The immediate parent of the node in the shortest-path tree
	** @param inc The incoming neighbours of the node
	** @throws IllegalArgumentException if the scheme or {@code inc} contains
	**         {@code tag}, or if either of them do not contain {@code parent}.
	** @throws IllegalStateException if the scheme has been made incomplete
	** @throws NullPointerException if {@code parent} is {@code null}
	*/
	public void pushNode(U2<T, A> node, T parent, Set<T> inc) {
		if (node_map.containsKey(node)) {
			throw new IllegalArgumentException("scheme already contains node " + node);
		}
		if (!node_map.K0Map().containsKey(parent)) {
			throw new IllegalArgumentException("scheme does not already contain parent " + parent);
		}
		if (inc.contains(node)) {
			throw new IllegalArgumentException("incoming set for " + node + " defines a loop");
		}
		if (!inc.contains(parent)) {
			throw new IllegalArgumentException("incoming set for " + node + " does not define parent " + parent);
		}
		if (incomplete) {
			throw new IllegalStateException("scheme set to incomplete");
		}
		if (parent == null) {
			throw new NullPointerException();
		}

		int i = node_list.size();
		Set<T> tinc = new HashSet<T>();
		Set<T> tpre = new HashSet<T>();
		List<T> opth = path.K0Map().get(parent);
		Object[] optha = opth.toArray(new Object[opth.size()+1]);
		optha[optha.length-1] = parent;
		@SuppressWarnings("unchecked") List<T> tpth = (List<T>)Arrays.asList(optha);

		node_list.add(node);
		node_map.put(node, i);
		if (node.type == 0) { outgoing.put(node.getT0(), new HashSet<U2<T, A>>()); }
		incoming.put(node, tinc);
		ancestor.put(node, tpre);
		path.put(node, tpth);

		if (inc.isEmpty()) { return; }
		for (T t: inc) {
			if (outgoing.containsKey(t)) {
				outgoing.get(t).add(node);
				tinc.add(t);
				tpre.addAll(ancestor.K0Map().get(t));
			}
		}

		if (nearest_tgraph == null && node.isT1()) {
			nearest_tgraph = node.getT1();
		}
	}

	/**
	** @see #pushNode(Union.U2, Object, Set)
	*/
	public void pushNodeT(T tag, T parent, Set<T> inc) {
		pushNode(Union.<T, A>U2_0(tag), parent, inc);
	}

	/**
	** @see #pushNode(Union.U2, Object, Set)
	*/
	public void pushNodeG(A addr, T parent, Set<T> inc) {
		pushNode(Union.<T, A>U2_1(addr), parent, inc);
	}

	public void setTagAttribute(T tag, W attr) {
		arc_attr_map.put(tag, attr);
	}

}
