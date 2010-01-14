// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.proto.LocalTGraph;
import tags.proto.TGraph;
import tags.proto.AddressScheme;

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

	final protected Map<A, LocalTGraph<T, U, W>> source;

	final protected Map<A, S> score;

	final protected TGraph<T, U, W> graph;

	final protected AddressScheme<T> scheme;

	public Naming() {
		// TODO NOW
		source = null;
		score = null;
		graph = null;
		scheme = null;
	}

}
