// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Maps.U2Map;

/**
** Local view of an {@link Index}.
**
** @param <T> Type of tag
** @param <A> Type of index address
** @param <W> Type of arc-attribute
*/
public class LocalIndex<T, A, W> extends Index<T, A, W> {

	// TODO NOW not entirely sure what this will be for, yet ¬.¬
	// we'll see what's needed when we code up Routing.
	final protected Set<A> src;

	/**
	** Map of nodes (documents and indexes) to their incoming tags and their
	** arc-weights.
	*/
	final protected U2Map<A, A, Map<T, W>> incoming = Maps.uniteDisjoint(new HashMap<A, Map<T, W>>(), new HashMap<A, Map<T, W>>());

	public LocalIndex() {
		throw new UnsupportedOperationException("not implemented");
	}

	// implementation notes:
	//
	// a LocalIndex needs to link to a map of indexes (representing data sources in use)
	//
	// this is so we can distinguish between neighbouring indexes that are already being
	// used as a data source (and hide these from the search results), and new indexes
	// that are not already being used as a data source.

	/**
	** Returns the results for a given tag, filtering out the indexes that are
	** in use as a data source.
	*/
	public U2Map<A, A, W> getOutgoingTLeaf(T src) {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<T, W> getIncomingDarcAttrMap(A doc) {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<T, W> getIncomingHarcAttrMap(A idx) {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Load a given subject tag's outgoing arcs, and their weights.
	**
	** @param subj The tag
	** @param out A map of arc-targets to arc-weights
	*/
	public void setOutgoingT(T subj, U2Map<A, A, W> out) {
		throw new UnsupportedOperationException("not implemented");
		// we need to distinguish between arcs to indexes already used as a
		// data source, and new indexes
	}

	/**
	** Register an index as a data source. The index must already be an
	** outgoing target of some loaded tag.
	*/
	public void setDataSource(A index) {
		throw new UnsupportedOperationException("not implemented");
	}

}
