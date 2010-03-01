// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Maps;
import tags.util.Union;

import tags.util.Union.U2;
import tags.util.Maps.U2Map;
import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

import java.io.IOException;

/**
** Local view of a {@link TGraph}.
**
** This implementation provides O(1) lookup for a node's outgoing and incoming
** neighbours.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
*/
public class LocalTGraph<T, A, U, W> extends TGraph<T, A, U, W> {

	/**
	** Remote address of this local view
	*/
	final public A addr;

	/**
	** The {@link DataSources} collection that this local view is part of.
	*/
	final protected DataSources<A, LocalTGraph<T, A, U, W>, ?> src;

	/**
	** Map of nodes (tags and tgraphs) to their incoming tags and their
	** arc-weights.
	*/
	final protected U2Map<T, A, Map<T, W>> incoming = Maps.uniteDisjoint(new HashMap<T, Map<T, W>>(), new HashMap<A, Map<T, W>>());

	final protected Map<T, Set<U2<T, A>>> incomplete = new HashMap<T, Set<U2<T, A>>>();
	final protected Set<T> complete = new HashSet<T>();
	final protected Set<T> complete_immute = Collections.unmodifiableSet(complete);

	/**
	** Constructs a new local view of the given remote address, attached to the
	** given {@link DataSources} collection.
	**
	** @throws NullPointerException if either parameter is {@code null}
	*/
	public LocalTGraph(A addr, DataSources<A, LocalTGraph<T, A, U, W>, ?> src) {
		if (addr == null || src == null) { throw new NullPointerException(); }
		this.addr = addr;
		this.src = src;
	}

	/**
	** Returns the set of tags for which itself, its out-arcs and its out-nodes
	** have all been loaded.
	*/
	public Set<T> getCompletedTags() {
		// TODO HIGH this might need to contain tags that we have loaded, that are NOT part of the TGraph
		return complete_immute;
	}

	/**
	** Returns the incoming neighbours of the given target tag. Only loaded
	** neighours (both node and arc) will be returned.
	*/
	public Neighbour<T, A, U, W> getIncomingT(T dst) {
		// FIXME NORM should really be immutable view
		Map<T, W> in_arc = incoming.K0Map().get(dst);
		Map<T, U> in_node = Maps.viewSubMap(node_map.K0Map(), in_arc.keySet());
		return new Neighbour<T, A, U, W>(in_node, Collections.<A, U>emptyMap(), in_arc, Collections.<A, W>emptyMap());
	}

	/**
	** Returns the incoming neighbours of the given target tgraph. Only loaded
	** neighbours (both node and arc) will be returned.
	*/
	public Neighbour<T, A, U, W> getIncomingG(A dst) {
		// FIXME NORM should really be immutable view
		Map<T, W> in_arc = incoming.K1Map().get(dst);
		Map<T, U> in_node = Maps.viewSubMap(node_map.K0Map(), in_arc.keySet());
		return new Neighbour<T, A, U, W>(in_node, Collections.<A, U>emptyMap(), in_arc, Collections.<A, W>emptyMap());
	}

	/**
	** Load a given subject tag, and its node-weight.
	**
	** '''NOTE''': This should be called for every single node loaded from the
	** remote structure; if the node was not found, call this with {@code null}
	** for the weight.
	**
	** @throws IllegalStateException if the tag is already loaded
	** @throws IOException if the remote data structure is inconsistent
	*/
	public void setNodeAttrT(T subj, U wgt) throws IOException {
		if (node_map.K0Map().containsKey(subj)) { throw new IllegalStateException("already loaded tag " + subj); }
		if (wgt == null) {
			if (incoming.K0Map().containsKey(subj)) { throw new IOException("corrupt remote data: node " + subj + " has defined incoming-arcs but undefined node-weight"); }
			// TODO HIGH this needs to be put somewhere else too
			return;
		}
		node_map.put(updateCompletion(Union.<T, A>U2_0(subj)), wgt);
	}

	/**
	** Load a given subject tgraph, and its node-weight.
	**
	** '''NOTE''': This should be called for every single node loaded from the
	** remote structure; if the node was not found, call this with {@code null}
	** for the weight.
	**
	** @throws IllegalStateException if the tag is already loaded
	** @throws IOException if the remote data structure is inconsistent
	*/
	public void setNodeAttrG(A subj, U wgt) throws IOException {
		if (node_map.K1Map().containsKey(subj)) { throw new IllegalStateException("already loaded tgraph " + subj); }
		if (wgt == null) {
			if (incoming.K1Map().containsKey(subj)) { throw new IOException("corrupt remote data: node " + subj + " has defined incoming-arcs but undefined node-weight"); }
			// TODO HIGH this needs to be put somewhere else too
			return;
		}
		node_map.put(updateCompletion(Union.<T, A>U2_1(subj)), wgt);
	}

	/**
	** Load a given subject tag's outgoing arcs, and their weights. This must
	** be called '''after''' {@link #setNodeAttrT(Object, Object)}.
	**
	** @param subj The tag
	** @param out A map of arc-targets to arc-weights
	** @throws IllegalStateException if the out-arcs are already loaded or if
	**         the node-weight is not already loaded
	** @throws IOException if the remote data structure is inconsistent
	*/
	public void setOutgoingT(T subj, U2Map<T, A, W> out) throws IOException {
		if (outgoing.containsKey(subj)) { throw new IllegalStateException("already loaded out-arcs for tag " + subj); }
		if (false /* TODO HIGH node_weight is already loaded and ABSENT */) {
			if (out != null) { throw new IOException("corrupt remote data: node " + subj + " has defined outgoing-arcs but undefined node-weight"); }
		} else if (!node_map.K0Map().containsKey(subj)) {
			throw new IllegalStateException("must first load node-weight for tag " + subj);
		}

		outgoing.put(subj, out);
		src.setOutgoing(addr, out.K1Map().keySet());

		// calculate incoming arcs
		for (Map.Entry<U2<T, A>, W> en: out.entrySet()) {
			U2<T, A> node = en.getKey();
			W wgt = en.getValue();
			Map<T, W> inc = incoming.get(node);
			if (inc == null) {
				incoming.put(node, inc = new HashMap<T, W>());
			}
			inc.put(subj, wgt);
		}

		// calculate incomplete neighbours
		putIncomplete(subj, out);
	}

	/**
	** Updates the completion status of all incoming neighbours of the subject
	** node.
	*/
	protected U2<T, A> updateCompletion(U2<T, A> subj) {
		if (subj == null) { throw new NullPointerException(); }

		Map<T, W> incm = incoming.get(subj);
		if (incm == null) {
			incoming.put(subj, incm = new HashMap<T, W>());
		}
		Set<T> inc = incm.keySet();

		if (inc == null) { return subj; }
		for (T tag: inc) {
			Set<U2<T, A>> inp = incomplete.get(tag);
			assert inp.contains(subj);
			inp.remove(subj);
			if (inp.isEmpty()) {
				incomplete.remove(tag);
				complete.add(tag);
			}
		}

		return subj;
	}

	/**
	** Initialises the completion status of the given subject tag.
	*/
	protected Set<U2<T, A>> putIncomplete(T subj, U2Map<T, A, W> out) {
		if (subj == null) { throw new NullPointerException(); }
		assert !incomplete.containsKey(subj) && !complete.contains(subj);

		// get the set of outgoing nodes that haven't been loaded
		Set<U2<T, A>> inp = new HashSet<U2<T, A>>(out.keySet());
		inp.removeAll(node_map.keySet());

		if (inp.isEmpty()) {
			complete.add(subj);
		} else {
			incomplete.put(subj, inp);
		}

		return inp;
	}

	private static LocalViewFactory stdFactory = new LocalViewFactory() {
		@SuppressWarnings("unchecked")
		@Override public Object createLocalView(Object addr, DataSources src) {
			return new LocalTGraph(addr, src);
		}
	};

	@SuppressWarnings("unchecked") public static <T, A, U, W> LocalViewFactory<A, LocalTGraph<T, A, U, W>> getFactory() {
		return (LocalViewFactory<A, LocalTGraph<T, A, U, W>>)stdFactory;
	}

}
