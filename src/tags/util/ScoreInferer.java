// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Set;
import java.util.Map;

/**
** An object for scoring nodes in a data network.
**
** @param <S> Type of score (weight of endorsement)
*/
public interface ScoreInferer<S> {

	/**
	** Returns an inferred endorsement for a node, given a set of seed nodes
	** for which the endorsements are already known, and the link structure of
	** the network of nodes.
	**
	** @param incoming Map of nodes to their incoming neighbours
	** @param seeds Map of seed nodes to their scores
	** @param subj Node to infer a score for
	*/
	public <A> S inferScore(Map<A, Set<A>> incoming, Map<A, S> seeds, A subj);

}
