// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import java.util.Set;

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

}
