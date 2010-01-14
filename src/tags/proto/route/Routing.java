// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.LocalIndex;

import java.util.Set;
import java.util.Map;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public class Routing<T, A, W, S> {

	final protected Map<A, LocalIndex<T, A, W>> source;

	final protected Map<A, S> score;

	final protected Map<A, Set<T>> lookup;

	final protected Map<A, LocalIndex<T, A, W>> result;

	public Routing() {
		// TODO NOW
		source = null;
		score = null;
		lookup = null;
		result = null;
	}

}
