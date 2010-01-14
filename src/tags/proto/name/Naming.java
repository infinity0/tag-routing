// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.proto.LocalTGraph;
import tags.proto.TGraph;
import tags.proto.AddressScheme;
import tags.store.StoreControl;

import java.util.Map;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public class Naming<T, A, U, W, S> {

	final protected StoreControl<?, T, A, U, W, S, ?> sctl;

	final protected Map<A, LocalTGraph<T, U, W>> source;
	final protected Map<A, S> score;
	final protected TGraph<T, U, W> graph;
	final protected AddressScheme<T> scheme;

	public Naming(StoreControl<?, T, A, U, W, S, ?> sctl) {
		this.sctl = sctl;
		// TODO NOW
		this.source = null;
		this.score = null;
		this.graph = null;
		this.scheme = null;
	}

}
