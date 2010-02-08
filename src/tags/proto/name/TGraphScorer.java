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

	/**
	** Infers a score for a given non-seed subject node.
	**
	** @param source Map of nodes to their local views
	** @param seeds Map of seed nodes to their scores
	** @param subj Node to infer a score for
	*/
	public <T, A> S getScoreFor(Map<A, LocalTGraph<T, A, U, W>> source, Map<A, S> seeds, A subj);

}
