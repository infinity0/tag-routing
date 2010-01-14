// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import java.util.Comparator;

/**
** DOCUMENT.
**
** @param <D> Type of distance
** @param <T> Type of tag
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
*/
public interface DistanceMetric<D, T, U, W> extends Comparator<D> {

	public D getDistance(T src, T dst, U srcw, U dstw, W arcw);

	// public int compare(D d, D d); from Comparator<D>

}
