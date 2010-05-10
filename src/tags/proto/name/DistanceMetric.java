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

	/**
	** Returns the distance between a node and itself.
	*/
	public D identity();

	/**
	** Returns the distance between a node and another node unreachable from
	** it.
	*/
	public D infinity();

	/**
	** Returns the distance between two nodes.
	*/
	public D getDistance(U srcu, U dstu, W arcw);

	/**
	** Combines two distances.
	*/
	public D combine(D d0, D d1);

	/**
	** Returns an attribute for an inferred arc between the seed node and the
	** subject node, given the distance between them.
	*/
	public W getAttrFromDistance(U srcu, U subju, D dist);

	/**
	** {@inheritDoc}
	**
	** Smaller is nearer.
	*/
	public int compare(D d0, D d1);

	/**
	** FIXME HIGH ugly hack, remove this when we add DistanceMetric to be a part of AddressScheme
	*/
	public W getSeedAttr();

}
