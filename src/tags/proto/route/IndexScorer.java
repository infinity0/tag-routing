// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.LocalIndex;

import java.util.Map;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public interface IndexScorer<T, A, W, S> {

	public S getScoreFor(LocalIndex<T, A, W> view, Map<LocalIndex<T, A, W>, S> sources);

}