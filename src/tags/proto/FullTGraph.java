// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Graphs;
import tags.util.Maps;

import tags.util.Arc;
import tags.util.Union.U2;
import tags.util.Maps.U2Map;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
** On top of {@link TGraph}, this class provides O(1) lookup for a node's
** incoming neighbours.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
*/
public class FullTGraph<T, A, U, W> extends TGraph<T, A, U, W> {

	/**
	** Map of nodes (tags and tgraphs) to their incoming tags and their
	** arc-weights.
	*/
	final protected U2Map<T, A, Map<T, W>> incoming = Maps.uniteDisjoint(new HashMap<T, Map<T, W>>(), new HashMap<A, Map<T, W>>());

	/**
	** Creates a new empty tag-graph.
	*/
	public FullTGraph() { }

	public FullTGraph(U2Map<T, A, U> node_map, U2Map<Arc<T, T>, Arc<T, A>, W> arc_map) {
		this.node_map.putAll(node_map);
		Graphs.populateFromNodesAndArcs(
			node_map.K0Map().keySet(),
			node_map.K0Map().keySet(),
			node_map.K1Map().keySet(),
			arc_map, this.outgoing, this.incoming
		);
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

}
