// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.proto.LocalTGraph;

import tags.util.Maps.MapX2;
import tags.util.Union.U2;
import tags.util.Arc;
import java.util.Map;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public interface TGraphComposer<T, A, U, W, S> {

	/**
	** Infer an attribute for the given node, from the given data sources.
	*/
	public U composeNode(MapX2<A, LocalTGraph<T, A, U, W>, S> src_score, U2<T, A> node);

	/**
	** Infer an attribute for the given arc, from the given data sources.
	*/
	public W composeArc(MapX2<A, LocalTGraph<T, A, U, W>, S> src_score, U2<Arc<T, T>, Arc<T, A>> arc);

}
