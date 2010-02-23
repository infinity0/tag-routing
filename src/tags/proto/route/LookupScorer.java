// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.LocalIndex;
import tags.proto.Lookup;
import tags.proto.AddressScheme;

import java.util.List;
import java.util.Set;
import java.util.Map;

/**
** DOCUMENT.
**
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public interface LookupScorer<W, S> {

	public <T, A> List<Lookup<T, A>> rankLookups(Map<LocalIndex<T, A, W>, Set<T>> lookups, AddressScheme<T, A, W> scheme);

	/**
	** Returns the arc-attribute ...
	**
	** @param idxs Score of index
	** @param tagw Attribute of seed-subject arc
	*/
	public W getLookupScore(S idxs, W tagw);

	/**
	** Returns the arc-attribute w.r.t. the seed tag, given the arc-attributes
	** related to some other subject tag.
	**
	** @param tagw Attribute of seed-subject arc
	** @param docw Attribute of subject-document arc
	*/
	public W getResultAttr(W tagw, W docw);

}
