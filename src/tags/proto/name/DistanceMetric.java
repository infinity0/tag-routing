// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import java.util.Comparator;

/**
** DOCUMENT. Distance between two tags. The type of "distance" should be such
** that there is a "minimum" or "zero" element.
**
** @param <D> Type of distance
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
*/
public interface DistanceMetric<D, U, W> extends Comparator<D> {

	public D getDistance(U srcw, U dstw, W arcw);

	public D getMinElement();

	public D getMaxElement();

	public D combine(D d1, D d2);

	/**
	** {@inheritDoc}
	**
	** Smaller is nearer.
	*/
	public int compare(D d1, D d2);

}
