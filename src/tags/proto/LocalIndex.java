// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Maps;

import tags.util.Maps.U2Map;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
** Local view of an {@link Index}.
**
** @param <T> Type of tag
** @param <A> Type of document/index address
** @param <W> Type of arc-attribute
*/
public class LocalIndex<T, A, W> extends Index<T, A, W> {

	/**
	** Remote address of this local view.
	*/
	final public A addr;

	/**
	** The {@link DataSources} collection that this local view is part of.
	*/
	final protected DataSources<A, LocalIndex<T, A, W>> src;

	// TODO NOW not entirely sure what this will be for, yet ¬.¬
	// we'll see what's needed when we code up Routing.
	// final protected Set<A> src;

	/**
	** Map of nodes (documents and indexes) to their incoming tags and their
	** arc-weights.
	*/
	final protected U2Map<A, A, Map<T, W>> incoming = Maps.uniteDisjoint(new HashMap<A, Map<T, W>>(), new HashMap<A, Map<T, W>>());

	public LocalIndex(A addr, DataSources<A, LocalIndex<T, A, W>> src) {
		this.addr = addr;
		this.src = src;
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

	private static LocalViewFactory stdFactory = new LocalViewFactory() {
		@Override @SuppressWarnings("unchecked") public Object createLocalView(Object addr, DataSources src) {
			return new LocalIndex(addr, src);
		}
	};

	@SuppressWarnings("unchecked") public static <T, A, W> LocalViewFactory<A, LocalIndex<T, A, W>> getFactory() {
		return (LocalViewFactory<A, LocalIndex<T, A, W>>)stdFactory;
	}

}
