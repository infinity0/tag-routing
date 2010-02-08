// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Maps;
import tags.util.Maps.U2Map;
import tags.util.Union.U2;
import tags.util.Union;

import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

/**
** Local view of a {@link TGraph}.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
*/
public class LocalTGraph<T, A, U, W> extends TGraph<T, A, U, W> {

	final protected U2Map<T, A, U> node_map = Maps.unionDisjoint(new HashMap<T, U>(), new HashMap<A, U>());
	final protected Map<T, U2Map<T, A, W>> outgoing = new HashMap<T, U2Map<T, A, W>>();
	final protected U2Map<T, A, Map<T, W>> incoming = Maps.unionDisjoint(new HashMap<T, Map<T, W>>(), new HashMap<A, Map<T, W>>());

	Map<T, Set<U2<T, A>>> incomplete = new HashMap<T, Set<U2<T, A>>>();
	Set<T> complete = new HashSet<T>();
	Set<T> complete_immute = Collections.unmodifiableSet(complete);

	/**
	** Returns the set of tags for which itself, its out-arcs and its out-nodes
	** have all been loaded.
	*/
	public Set<T> getCompletedTags() {
		return complete_immute;
	}

	protected U2<T, A> updateCompletion(U2<T, A> subj) {
		if (subj == null) { throw new NullPointerException(); }

		Set<T> inc = incoming.get(subj).keySet();

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

	protected Set<U2<T, A>> putIncomplete(T subj, U2Map<T, A, W> out) {
		if (subj == null) { throw new NullPointerException(); }
		assert !incomplete.containsKey(subj) && !complete.contains(subj);

		// get the set of outgoing nodes that haven't been loaded
		// FIXME problem here with inconsistent data structures
		Set<U2<T, A>> inp = new HashSet<U2<T, A>>(out.keySet());
		inp.removeAll(node_map.keySet());

		if (inp.isEmpty()) {
			complete.add(subj);
		} else {
			incomplete.put(subj, inp);
		}

		return inp;
	}

	/**
	** Returns the incoming neighbours of the given target tag. Only loaded
	** neighours will be returned.
	*/
	public Neighbour<T, A, U, W> getIncomingT(T dst) {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Returns the incoming neighbours of the given target tgraph. Only loaded
	** neighbours will be returned.
	*/
	public Neighbour<T, A, U, W> getIncomingG(A dst) {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Load a given subject tag, and its node-weight.
	*/
	public void setNodeAttrT(T subj, U wgt) {
		node_map.put(updateCompletion(Union.<T, A>U2_0(subj)), wgt);
	}

	/**
	** Load a given subject tgraph, and its node-weight.
	*/
	public void setNodeAttrG(A subj, U wgt) {
		node_map.put(updateCompletion(Union.<T, A>U2_1(subj)), wgt);
	}

	/**
	** Load a given subject tag's outgoing arcs, and their weights
	**
	** @param subj The tag
	** @param out A map of arc-targets to arc-weights
	*/
	public void setOutgoingT(T subj, U2Map<T, A, W> out) {
		if (outgoing.containsKey(subj)) {
			throw new IllegalArgumentException("already loaded tag " + subj);
		}

		outgoing.put(subj, out);

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

}
