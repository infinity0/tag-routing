// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.init;

import tags.proto.PTable;

import java.util.Map;

/**
** DOCUMENT.
**
** @param <A> Type of address
** @param <S> Type of score
** @param <R> Type of identity-score
*/
public class Init<A, S, R> {

	// TODO NORM maybe have <I> as well

	final protected Map<PTable<A, S>, R> source;

	public Init() {
		// TODO NOW
		source = null;
	}

}