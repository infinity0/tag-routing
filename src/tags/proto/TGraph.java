// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Maps.U2Map;
import tags.util.Maps.U2MapX2;

import java.util.Set;
import java.util.Map;
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

	/**
	** Return a view of the nodes of this graph, each mapped to its attribute.
	*/
	public U2Map<T, A, U> nodeMap() {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Return a view of the arcs of this graph, each mapped to its attribute.
	*/
	public U2Map<Arc<T, T>, Arc<T, A>, W> arcMap() {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Return the outgoing neighbours of the given source tag.
	*/
	public Neighbour<T, A, U, W> getOutgoingT(T src) {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** A class to view the neighbouring arcs/nodes of a given node.
	*/
	abstract public static class Neighbour<T, A, U, W> {

		final protected TGraph<T, A, U, W> graph;
		final T subj;

		public Neighbour(TGraph<T, A, U, W> graph, T subj) {
			this.graph = graph;
			this.subj = subj;
		}

		/**
		** Return a view of the subject's neighbours, each mapped to its own
		** attribute, and the attribute of the arc between it and the subject.
		*/
		abstract public U2MapX2<T, A, U, W> attrMap();

		/**
		** Return a view of the subject's neighbours, each mapped to its own
		** attribute.
		*/
		abstract public U2Map<T, A, U> nodeAttrMap();

		/**
		** Return a view of the subject's neighbours, each mapped to the
		** attribute of the arc between it and the subject.
		*/
		abstract public U2Map<T, A, W> arcAttrMap();

	}

}
