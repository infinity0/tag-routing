// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import java.util.Set;
import java.util.Map;

/**
** Tag graph. DOCUMENT.
**
** @param <T> Type of tag
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
*/
public class TGraph<T, U, W> {

	public U getTagWeight(T tag) {
		throw new UnsupportedOperationException("not implemented");
	}

	public W getArcWeight(T src, T dst) {
		throw new UnsupportedOperationException("not implemented");
	}

	public Set<T> getOutgoing(T src) {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<T, U> getOutgoingTagWeights(T src) {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<T, W> getOutgoingArcWeights(T src) {
		throw new UnsupportedOperationException("not implemented");
	}

}
