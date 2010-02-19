// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Maps;
import tags.util.Union;
import java.util.Collections;

import tags.util.Union.U2;
import tags.util.Maps.U2Map;
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
*/
public class AddressScheme<T, A> {

	final protected List<U2<T, A>> node_list = new ArrayList<U2<T, A>>();
	final protected U2Map<T, A, Integer> node_map = Maps.uniteDisjoint(new HashMap<T, Integer>(), new HashMap<A, Integer>());

	final protected Map<T, Set<U2<T, A>>> outgoing = new HashMap<T, Set<U2<T, A>>>();
	final protected U2Map<T, A, Set<T>> incoming = Maps.uniteDisjoint(new HashMap<T, Set<T>>(), new HashMap<A, Set<T>>());
	final protected U2Map<T, A, Set<T>> preceding = Maps.uniteDisjoint(new HashMap<T, Set<T>>(), new HashMap<A, Set<T>>());

	/**
	** Whether the scheme is complete for the backing graph. If this is {@code
	** true}, then further information could be extracted by completing a tag
	** in the backing graph (ie. loading it and all its out-arcs, out-nodes).
	*/
	protected boolean incomplete = false;

	/**
	** Construct a new scheme with the given tag as the zeroth (ie. seed) tag.
	*/
	public AddressScheme(T src) {
		pushNodeT(src, null);
	}

	public T getSeedTag() {
		U2<T, A> zero = node_list.get(0);
		return zero.getT0();
	}

	public boolean isIncomplete() {
		return incomplete;
	}

	public void setIncomplete() {
		incomplete = true;
	}

	// TODO HIGH code some getter methods for these

	public Set<T> getAllTags() {
		// FIXME NORM should really be immutable view
		return outgoing.keySet();
	}

	/**
	** Returns all tags nearer than the given node, which lie on a greedy path
	** between it and the seed tag.
	*/
	public Set<T> getPreceding(U2<T, A> node) {
		return Collections.unmodifiableSet(preceding.get(node));
	}

	public Set<T> getPrecedingT(T tag) {
		return Collections.unmodifiableSet(preceding.K0Map().get(tag));
	}

	public void pushNodeT(T tag, Set<T> inc) {
		pushNode(Union.<T, A>U2_0(tag), inc);
	}

	public void pushNodeG(A addr, Set<T> inc) {
		pushNode(Union.<T, A>U2_1(addr), inc);
	}

	/**
	** Attaches the given node to the address scheme, with a set of incoming
	** neighbours. Only nodes already in the scheme (ie. nearer to the seed)
	** will be added as incoming neighbours; the rest will be filtered out.
	**
	** @param node The node to push onto this address scheme
	** @param inc The incoming neighbours of the node
	** @throws IllegalArgumentException if the scheme already contains {@code
	**         tag}, or if {@code inc} contains it
	*/
	public void pushNode(U2<T, A> node, Set<T> inc) {
		if (node_map.containsKey(node)) {
			throw new IllegalArgumentException("scheme already contains node: " + node);
		}
		if (inc.contains(node)) {
			throw new IllegalArgumentException("cannot accept an incoming set that defines a loop");
		}

		int i = node_list.size();
		Set<T> tinc = new HashSet<T>();
		Set<T> tpre = new HashSet<T>();

		node_list.add(node);
		node_map.put(node, i);
		if (node.type == 0) { outgoing.put(node.getT0(), new HashSet<U2<T, A>>()); }
		incoming.put(node, tinc);
		preceding.put(node, tpre);

		if (inc == null || inc.isEmpty()) { return; }
		for (T t: inc) {
			if (outgoing.containsKey(t)) {
				outgoing.get(t).add(node);
				tinc.add(t);
				tpre.addAll(preceding.K0Map().get(t));
			}
		}
	}

}
