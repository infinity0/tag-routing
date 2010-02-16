// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

/**
** Local view of an {@link Index}.
**
** @param <T> Type of tag
** @param <A> Type of index address
** @param <W> Type of arc-attribute
*/
public class LocalIndex<T, A, W> extends Index<T, A, W> {


	// implementation notes:
	//
	// a LocalIndex needs to link to a map of indexes (representing data sources in use)
	//
	// this is so we can distinguish between neighbouring indexes that are already being
	// used as a data source (and hide these from the search results), and new indexes
	// that are not already being used as a data source.

	/**
	** Load a given subject tag's outgoing arcs, and their weights
	**
	** @param subj The tag
	** @param out A map of arc-targets to arc-weights
	*/
	public void setOutgoingT(T subj, U2Map<T, A, W> out) {
		throw new UnsupportedOperationException();
		// we need to distinguish between arcs to indexes already used as a
		// data source, and new indexes
	}

}
