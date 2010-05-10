// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.Index.Lookup;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

/**
** DOCUMENT.
**
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public interface LookupScorer<W, S> {

	/**
	** Returns a score for a lookup.
	**
	** TODO HIGH. this needs to take into account the index-tag attribute, if
	** necessary inferring one.
	**
	** @param idxs Score of index
	** @param tagw Attribute of seed-subject arc
	*/
	public W getLookupScore(S idxs, W tagw);

	/**
	** DOCUMENT
	*/
	public W getPotential(Collection<W> lkuw);

	/**
	** Returns the arc-attribute w.r.t. the seed tag, given the arc-attributes
	** related to some other subject tag.
	**
	** @param tagw Attribute of seed-subject arc
	** @param docw Attribute of subject-document arc
	*/
	public W getResultAttr(W tagw, W docw);

}
