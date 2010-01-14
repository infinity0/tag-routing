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
** @param <T> Type of tag
** @param <A> Type of address
** @param <W> Type of arc-attribute
*/
public interface LookupRanker<T, A, W> {

	public List<Lookup<T, A>> rankLookups(Map<LocalIndex<T, A, W>, Set<T>> lookups, AddressScheme<Integer> scheme);

}
