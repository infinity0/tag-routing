// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.LocalIndex;
import tags.store.StoreControl;

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

	final protected StoreControl<?, T, A, ?, W, S, ?> sctl;

	final protected Map<A, LocalIndex<T, A, W>> source;
	final protected Map<A, S> score;
	final protected Map<A, Set<T>> lookup;
	final protected Map<A, LocalIndex<T, A, W>> result;

	public Routing(StoreControl<?, T, A, ?, W, S, ?> sctl) {
		this.sctl = sctl;
		// TODO NOW
		this.source = null;
		this.score = null;
		this.lookup = null;
		this.result = null;
	}

}
