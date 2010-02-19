// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.cont;

import tags.proto.PTable;
import tags.util.Maps.MapX2;
import java.util.Map;

/**
** DOCUMENT.
**
** @param <S> Type of score
** @param <R> Type of identity-score
*/
public interface PTableComposer<S, R> {

	/**
	** Returns the combined value for the given item (interpreted as a TGraph
	** address), using data from the given sources.
	*/
	public <I, A> S composePTableGNode(MapX2<I, PTable<A, S>, R> src_score, A item);

	/**
	** Returns the combined value for the given item (interpreted as an Index
	** address), using data from the given sources.
	*/
	public <I, A> S composePTableHNode(MapX2<I, PTable<A, S>, R> src_score, A item);

}
