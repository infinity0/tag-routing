// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Graphs;

import tags.util.Arc;
import tags.util.Union.U2;
import tags.util.Maps.U2Map;
import tags.util.Maps.U2MapX2;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

/**
** Index. DOCUMENT.
**
** Bi-partite graph from tags to documents/indexes.
**
** This implementation provides O(1) lookup for a tag's outgoing neighbours.
**
** @param <T> Type of tag
** @param <A> Type of index address
** @param <W> Type of arc-attribute
*/
public class Index<T, A, W> {

	/**
	** Map of tags to their outgoing nodes (documents and indexes) and their
	** arc-weights.
	*/
	final protected Map<T, U2Map<A, A, W>> outgoing = new HashMap<T, U2Map<A, A, W>>();

	final protected U2Map<Arc<T, A>, Arc<T, A>, W> arc_map_view = Graphs.viewAsArcMap(outgoing);

	/**
	** Set of indexes pointed to by this index.
	*/
	final protected Set<A> node_set_h = new HashSet<A>();

	/**
	** Set of documents pointed to by this index.
	*/
	final protected Set<A> node_set_d = new HashSet<A>();

	/**
	** Creates a new empty index.
	*/
	public Index() { }

	public Index(U2Map<Arc<T, A>, Arc<T, A>, W> arc_map) {
		Graphs.populateFromArcMap(arc_map, outgoing, null, new java.util.HashSet<T>(), node_set_d, node_set_h);
	}

	/**
	** Returns the set of tags referred to by this index.
	*/
	public Set<T> nodeSetT() {
		return outgoing.keySet();
	}

	/**
	** Returns the set of documents referred to by this index.
	*/
	public Set<A> nodeSetD() {
		// FIXME NORM should really be immutable view
		return node_set_d;
		//return incoming.K0Map().keySet();
	}

	/**
	** Returns the set of indexes referred to by this index.
	*/
	public Set<A> nodeSetH() {
		// FIXME NORM should really be immutable view
		return node_set_h;
		//return incoming.K1Map().keySet();
	}

	public U2Map<Arc<T, A>, Arc<T, A>, W> arcMap() {
		// FIXME NORM should really be immutable view
		return arc_map_view;
	}

	/**
	** Whether this object knows about both endpoints of the given arc. If this
	** is true, but the given arc does not belong to this object, then it is
	** likely that the two endpoints have an implicit "zero-arc" between them.
	*/
	public boolean hasEndpoints(U2<Arc<T, A>, Arc<T, A>> arc) {
		if (arc.isT0()) {
			Arc<T, A> a = arc.getT0();
			return nodeSetT().contains(a.src) && nodeSetH().contains(a.dst);
		} else {
			Arc<T, A> a = arc.getT1();
			return nodeSetT().contains(a.src) && nodeSetD().contains(a.dst);
		}
	}

	public U2Map<A, A, W> getOutgoingTarcAttrMap(T src) {
		return outgoing.get(src);
	}


	public static class Lookup<T, A> {

		final public A idx;
		final public T tag;

		public Lookup(A idx, T tag) {
			if (idx == null || tag == null) { throw new NullPointerException(); }
			this.idx = idx;
			this.tag = tag;
		}

		@Override public boolean equals(Object o) {
			if (o == this) { return true; }
			if (!(o instanceof Lookup)) { return false; }
			Lookup lku = (Lookup)o;
			return idx.equals(lku.idx) && tag.equals(lku.tag);
		}

		@Override public int hashCode() {
			return idx.hashCode() ^ tag.hashCode() ^ 1446548050;
		}

		@Override public String toString() {
			return "(" + idx + ": " + tag + ")";
		}

		public static <T, A> Lookup<T, A> make(A idx, T tag) {
			return new Lookup<T, A>(idx, tag);
		}

	}

}
