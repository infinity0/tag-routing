// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Graphs;
import tags.util.Maps;

import tags.util.Union.U2;
import tags.util.Maps.U2Map;
import tags.util.Arc;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
** On top of {@link Index}, this class provides O(1) lookup for a node's
** incoming neighbours.
**
** @param <T> Type of tag
** @param <A> Type of document/index address
** @param <W> Type of arc-attribute
*/
public class FullIndex<T, A, W> extends Index<T, A, W> {

	/**
	** Map of nodes (documents and indexes) to their incoming tags and their
	** arc-weights.
	*/
	final protected U2Map<A, A, Map<T, W>> incoming = Maps.uniteDisjoint(new HashMap<A, Map<T, W>>(), new HashMap<A, Map<T, W>>());

	/**
	** Creates a new empty index.
	*/
	public FullIndex() { }

	public FullIndex(U2Map<Arc<T, A>, Arc<T, A>, W> arc_map) {
		super(arc_map);
		Graphs.populateIncoming(arc_map, incoming);
	}

	public Map<T, W> getIncomingDarcAttrMap(A doc) {
		// FIXME NORM should really be immutable view
		return incoming.K0Map().get(doc);
	}

	public Map<T, W> getIncomingHarcAttrMap(A idx) {
		// FIXME NORM should really be immutable view
		return incoming.K1Map().get(idx);
	}

}
