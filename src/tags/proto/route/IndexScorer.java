// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.LocalIndex;

import java.util.Map;

/**
** DOCUMENT.
**
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public interface IndexScorer<W, S> {

	public <T, A> S getScoreFor(LocalIndex<T, A, W> view, Map<LocalIndex<T, A, W>, S> sources);

}
