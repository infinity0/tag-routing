// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import java.util.Comparator;

/**
** DOCUMENT.
**
** @param <D> Type of distance
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
*/
public interface DistanceMetric<D, U, W> extends Comparator<D> {

	public D getDistance(U srcw, U dstw, W arcw);

	// public int compare(D d, D d); from Comparator<D>

}
