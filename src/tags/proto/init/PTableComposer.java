// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.init;

import tags.proto.PTable;

import java.util.Map;

/**
** DOCUMENT.
**
** @param <S> Type of score
** @param <R> Type of identity-score
*/
public interface PTableComposer<S, R> {

	public <A> S composePTableNode(Map<PTable<A, S>, R> src_score, A item);

}
