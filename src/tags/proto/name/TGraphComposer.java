// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.proto.LocalTGraph;

import java.util.Map;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public interface TGraphComposer<T, U, W, S> {

	public U composeTGraphNode(Map<LocalTGraph<T, U, W>, S> sources, T tag);

	public W composeTGraphArc(Map<LocalTGraph<T, U, W>, S> sources, T src, T dst);

}
