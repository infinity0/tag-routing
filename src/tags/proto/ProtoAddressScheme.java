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

	/**
	** Construct a new scheme with the given tag as the zeroth (ie. seed) tag.
	** If {@code cmp} is used, then {@link #getMostRelevant(Iterable)} will use
	** the natural ordering for {@code <W>} (it must be {@link Comparable}).
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

	@Override public boolean isIncomplete() {
		return incomplete;
	}

	@Override public void setIncomplete() {
		incomplete = true;
	}

	@Override public T seedTag() {
		U2<T, A> zero = node_list.get(0);
		return zero.getT0();
	}

	/**
	** Returns the tag with the highest arc-attribute (ie. opposite of default
	** java sort order), or {@code null} if none of the tags have an attribute
	** defined.
	*/
	@Override public T getMostRelevant(Set<T> tags) {
		Map.Entry<T, W> max = Collections.max(Maps.viewSubMap(arc_attr_map, tags).entrySet(), (cmp == null)?
		new Comparator<Map.Entry<T, W>>() {
			@SuppressWarnings("unchecked")
			@Override public int compare(Map.Entry<T, W> en0, Map.Entry<T, W> en1) {
				return ((Comparable<W>)en0.getValue()).compareTo(en1.getValue());
			}
		}:
		new Comparator<Map.Entry<T, W>>() {
			@Override public int compare(Map.Entry<T, W> en0, Map.Entry<T, W> en1) {
				return cmp.compare(en0.getValue(), en1.getValue());
			}
		});
		return max.getKey();
	}

	@Override public Set<T> tagSet() {
		return arc_attr_map.keySet();
	}

	@Override public Map<T, W> arcAttrMap() {
		// FIXME NORM should really be immutable view
		return arc_attr_map;
	}

	@Override public List<U2<T, A>> nodeList() {
		// FIXME NORM should really be immutable view
		return node_list;
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

	@Override public void pushNode(U2<T, A> node, T parent, Set<T> inc) {
		if (node_map.containsKey(node)) {
			throw new IllegalArgumentException("scheme already contains node: " + node);
		}
		if (!node_map.K0Map().containsKey(parent)) {
			throw new IllegalArgumentException("scheme does not already contain parent: " + parent);
		}
		if (inc.contains(node)) {
			throw new IllegalArgumentException("incoming set defines a loop for: " + node);
		}
		if (!inc.contains(parent)) {
			throw new IllegalArgumentException("incoming set does not define: " + parent);
		}
		if (parent == null) {
			throw new NullPointerException();
		}

		int i = node_list.size();
		Set<T> tinc = new HashSet<T>();
		Set<T> tpre = new HashSet<T>();
		List<T> opth = path.get(parent);
		Object[] optha = opth.toArray(new Object[opth.size()+1]);
		optha[optha.length] = parent;
		@SuppressWarnings("unchecked") List<T> tpth = (List<T>)Arrays.asList(optha);

		node_list.add(node);
		node_map.put(node, i);
		if (node.type == 0) { outgoing.put(node.getT0(), new HashSet<U2<T, A>>()); }
		incoming.put(node, tinc);
		ancestor.put(node, tpre);
		path.put(node, tpth);

		if (inc == null || inc.isEmpty()) { return; }
		for (T t: inc) {
			if (outgoing.containsKey(t)) {
				outgoing.get(t).add(node);
				tinc.add(t);
				tpre.addAll(ancestor.K0Map().get(t));
			}
		}
	}

	@Override public void pushNodeT(T tag, T parent, Set<T> inc) {
		pushNode(Union.<T, A>U2_0(tag), parent, inc);
	}

	@Override public void pushNodeG(A addr, T parent, Set<T> inc) {
		pushNode(Union.<T, A>U2_1(addr), parent, inc);
	}

}
