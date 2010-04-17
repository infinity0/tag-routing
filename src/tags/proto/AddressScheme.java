// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Union.U2;
import tags.util.Maps.U2Map;
import java.util.Comparator;
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
public interface AddressScheme<T, A, W> extends Comparator<W> {

	/**
	** Return the seed tag.
	*/
	public T seedTag();

	/**
	** Returns the set of tags contained within this scheme.
	*/
	public Set<T> tagSet();

	/**
	** Returns a map of tags to their arc-attributes, relative to the seed tag.
	*/
	public Map<T, W> arcAttrMap();

	/**
	** Returns a list of nodes in ascending order from their shortest distance
	** to the seed tag.
	*/
	public List<U2<T, A>> nodeList();

	/**
	** Returns a map of nodes to their orderings in the shortest-distance list.
	*/
	public U2Map<T, A, Integer> indexMap();

	/**
	** Returns a map of nodes to the shortest path between it and the seed tag.
	** The path starts from the seed tag, and does not contain the node itself.
	*/
	public U2Map<T, A, List<T>> pathMap();

	/**
	** Returns a map of nodes to the set of all its ancestors.
	*/
	public U2Map<T, A, Set<T>> ancestorMap();

	/**
	** Whether the scheme is complete for the backing graph. If this is {@code
	** true}, then further information could be extracted by completing a tag
	** in the backing graph (ie. loading it and all its out-arcs, out-nodes).
	*/
	public boolean isIncomplete();

	public T getIncomplete();

	public Comparator<W> comparator();

	/**
	** @throws ClassCastException if the comparator is {@code null} and the
	**         elements have no natural ordering
	*/
	public int compare(W w0, W w1);

	/**
	** DOCUMENT. Returns {@code null} if {@code map} is empty
	*/
	public <K> Map.Entry<K, W> getMostRelevant(Map<K, W> map);

	/**
	** Returns the tag with the highest arc-attribute (ie. opposite of default
	** java sort order), or {@code null} if none of the tags have an attribute
	** defined.
	**
	** @throws IllegalArgumentException if the iterable contains a tag unknown
	**         to this address scheme
	** @throws NullPointerException if {@code tags} is {@code null}
	*/
	public Map.Entry<T, W> getMostRelevant(Set<T> tags);

	public A getNearestTGraph();

}
