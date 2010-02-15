// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Maps;
import tags.util.Maps.U2Map;
import tags.util.Maps.U2MapX2;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import tags.util.Arc;

/**
** Tag graph. DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
*/
public class TGraph<T, A, U, W> {

	final protected U2Map<T, A, U> node_map = Maps.uniteDisjoint(new HashMap<T, U>(), new HashMap<A, U>());
	final protected Map<T, U2Map<T, A, W>> outgoing = new HashMap<T, U2Map<T, A, W>>();

	// NOTE
	// node_map.K0Map().keySet() should always be the same as outgoing.keySet()

	/**
	** Returns a view of the nodes of this graph, each mapped to its attribute.
	*/
	public U2Map<T, A, U> nodeMap() {
		// FIXME NORM should really be immutable view
		return node_map;
	}

	/**
	** Returns a view of the arcs of this graph, each mapped to its attribute.
	*/
	public U2Map<Arc<T, T>, Arc<T, A>, W> arcMap() {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Returns the outgoing neighbours of the given source tag.
	*/
	public Neighbour<T, A, U, W> getOutgoingT(T src) {
		// FIXME NORM should really be immutable view
		Map<T, W> out_arc_0 = outgoing.get(src).K0Map();
		Map<A, W> out_arc_1 = outgoing.get(src).K1Map();
		Map<T, U> out_node_0 = Maps.viewSubMap(node_map.K0Map(), out_arc_0.keySet());
		Map<A, U> out_node_1 = Maps.viewSubMap(node_map.K1Map(), out_arc_1.keySet());
		return new Neighbour<T, A, U, W>(out_node_0, out_node_1, out_arc_0, out_arc_1);
	}

	/**
	** A class to view the neighbouring arcs/nodes of a given node.
	*/
	public static class Neighbour<T, A, U, W> {

		final U2MapX2<T, A, U, W> attr_map;
		final U2Map<T, A, U> node_attr_map;
		final U2Map<T, A, W> arc_attr_map;

		public Neighbour(Map<T, U> t_node, Map<A, U> g_node, Map<T, W> t_arc, Map<A, W> g_arc) {
			attr_map = Maps.convoluteStrictUniteDisjoint(t_node, g_node, t_arc, g_arc, Maps.BaseMapX2.Inclusion.SUB0SUP1);
			node_attr_map = Maps.uniteDisjoint(t_node, g_node);
			arc_attr_map = Maps.uniteDisjoint(t_arc, g_arc);
		}

		/**
		** Returns a view of the subject's neighbours, each mapped to its own
		** attribute, and the attribute of the arc between it and the subject.
		*/
		public U2MapX2<T, A, U, W> attrMap() {
			return attr_map;
		}

		/**
		** Returns a view of the subject's neighbours, each mapped to its own
		** attribute.
		*/
		public U2Map<T, A, U> nodeAttrMap() {
			return node_attr_map;
		}

		/**
		** Returns a view of the subject's neighbours, each mapped to the
		** attribute of the arc between it and the subject.
		*/
		public U2Map<T, A, W> arcAttrMap() {
			return arc_attr_map;
		}

	}

}
