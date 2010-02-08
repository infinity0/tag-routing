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

	/**
	** Infers a score for a given non-seed subject node.
	**
	** @param source Map of nodes to their local views
	** @param seeds Map of seed nodes to their scores
	** @param subj Node to infer a score for
	*/
	public <T, A> S getScoreFor(Map<A, LocalIndex<T, A, W>> source, Map<A, S> seeds, A subj);

}
