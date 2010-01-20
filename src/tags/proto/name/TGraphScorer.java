// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.proto.LocalTGraph;

import java.util.Map;

/**
** DOCUMENT.
**
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public interface TGraphScorer<U, W, S> {

	public <T, A> S getScoreFor(LocalTGraph<T, A, U, W> view, Map<LocalTGraph<T, A, U, W>, S> sources);

}
