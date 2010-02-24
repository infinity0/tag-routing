// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Maps;

import tags.util.Arc;
import tags.util.Maps.U2Map;
import tags.util.Maps.U2MapX2;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
** Index. DOCUMENT.
**
** Bi-partite graph from tags to indexes/documents.
**
** This implementation provides O(1) lookup for a tag's outgoing neighbours.
**
** @param <T> Type of tag
** @param <A> Type of index address
** @param <W> Type of arc-attribute
*/
public class Index<T, A, W> {

	// TODO NORM
	// may not be good idea to have the same type of address for both documents and indexes

	/**
	** Map of tags to their outgoing nodes (documents and indexes) and their
	** arc-weights.
	*/
	final protected Map<T, U2Map<A, A, W>> outgoing = new HashMap<T, U2Map<A, A, W>>();

	/**
	** Returns the set of tags referred to by this index.
	*/
	public Set<T> nodeSetT() {
		return outgoing.keySet();
	}

	/**
	** Returns the set of documents referred to by this index.
	*/
	public Set<A> nodeSetD() {
		// TODO HIGH maybe this need to go into LocalIndex
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Returns the set of indexes referred to by this index.
	*/
	public Set<A> nodeSetH() {
		// TODO HIGH maybe this need to go into LocalIndex
		throw new UnsupportedOperationException("not implemented");
	}

	public U2Map<Arc<T, A>, Arc<T, A>, W> arcMap() {
		throw new UnsupportedOperationException("not implemented");
	}

	public U2Map<A, A, W> getOutgoingTarcAttrMap(T src) {
		return outgoing.get(src);
	}

}
