// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.proto.AddressScheme;
import tags.proto.FullTGraph;
import java.util.Set;

/**
** DOCUMENT. Distance between two tags. The type of "distance" should be such
** that there is a "minimum" or "zero" element.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
*/
public interface AddressSchemeBuilder<T, A, U, W> {

	/**
	** Build a new {@link AddressScheme} from the given parameters.
	**
	** @param graph The composite graph
	** @param completed Set of completed tags
	** @param seed The seed tag
	*/
	public AddressScheme<T, A, W> buildAddressScheme(FullTGraph<T, A, U, W> graph, Set<T> completed, T seed);

}
