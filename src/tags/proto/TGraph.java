// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

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

	public Map<T, U> getTNodes() {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<A, U> getGNodes() {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<Arc<T, T>, W> getTArcs() {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<Arc<T, A>, W> getGArcs() {
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
		** Return a view of the subject's tag neighbours, each mapped to its
		** attribute.
		*/
		abstract public Map<T, U> getTNodes();

		/**
		** Return a view of the subject's tgraph neighbours, each mapped to its
		** attribute.
		*/
		abstract public Map<A, U> getGNodes();

		/**
		** Return a view of the subject's tag neighbours, each mapped to the
		** attribute of the arc between it and the subject tag.
		*/
		abstract public Map<T, W> getTArcs();

		/**
		** Return a view of the subject's tgraph neighbours, each mapped to the
		** attribute of the arc between it and the subject tag.
		*/
		abstract public Map<A, W> getGArcs();

	}

}
