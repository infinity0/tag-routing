// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import java.util.Set;
import java.util.Map;

/**
** Local view of a {@link TGraph}.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
*/
public class LocalTGraph<T, A, U, W> extends TGraph<T, A, U, W> {

	/**
	** Returns the set of tags for which itself, its out-arcs and its out-nodes
	** have all been loaded.
	*/
	public Set<T> getCompletedTags() {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Returns the incoming neighbours of the given target tag.
	*/
	public Neighbour<T, A, U, W> getIncomingT(T dst) {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Returns the incoming neighbours of the given target tgraph.
	*/
	public Neighbour<T, A, U, W> getIncomingG(A dst) {
		throw new UnsupportedOperationException("not implemented");
	}

	public void setTNodeAttr(T tag, U wgt) {
		throw new UnsupportedOperationException("not implemented");
	}

	public void setGNodeAttr(A tag, U wgt) {
		throw new UnsupportedOperationException("not implemented");
	}

	public void setTArcAttr(T src, T dst, W wgt) {
		throw new UnsupportedOperationException("not implemented");
	}

	public void setGArcAttr(T src, A dst, W wgt) {
		throw new UnsupportedOperationException("not implemented");
	}

}
