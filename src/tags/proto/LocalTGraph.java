// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import java.util.Set;
import java.util.Map;

/**
** Local view of a {@link TGraph}.
**
** @param <T> Type of tag
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
*/
public class LocalTGraph<T, U, W> extends TGraph<T, U, W> {

	/**
	** Returns the set of tags for which itself, its out-arcs and its out-nodes
	** have all been loaded.
	*/
	public Set<T> getCompletedTags() {
		throw new UnsupportedOperationException("not implemented");
	}

	public Set<T> getIncoming(T dst) {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<T, U> getIncomingTagWeights(T dst) {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<T, W> getIncomingArcWeights(T dst) {
		throw new UnsupportedOperationException("not implemented");
	}

	public void setTagWeight(T tag, U wgt) {
		throw new UnsupportedOperationException("not implemented");
	}

	public void setArcWeight(T src, T dst, W wgt) {
		throw new UnsupportedOperationException("not implemented");
	}

}
