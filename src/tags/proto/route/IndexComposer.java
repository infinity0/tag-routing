// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.LocalIndex;
import tags.util.Maps.MapX2;
import tags.util.Union.U2;
import tags.util.Arc;
import java.util.Map;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public interface IndexComposer<T, A, W, S> {

	/**
	** Infer an attribute for the given arc, from the given data sources.
	*/
	public W composeArc(MapX2<A, LocalIndex<T, A, W>, S> src_score, U2<Arc<T, A>, Arc<T, A>> arc);

}
