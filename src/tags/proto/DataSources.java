// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

/**
** Graph of remote data sources that point to each other. DOCUMENT.
**
** @param <R> Type of remote address
** @param <L> Type of local view
*/
public class DataSources<R, L> {

	final protected Map<R, L> seeds = new HashMap<R, L>();

	final protected Map<R, Set<R>> outgoing = new HashMap<R, Set<R>>();
	final protected Map<R, Set<R>> incoming = new HashMap<R, Set<R>>();

	final protected Map<R, L> in_use = new HashMap<R, L>();

	/**
	** Returns the set of seed sources, each mapped to its local view.
	*/
	public Map<R, L> getSeeds() {
		// FIXME NORM should really be immutable view
		return seeds;
	}

	/**
	** Set an outgoing arc for the given pair of sources.
	*/
	public void setOutgoing(R src, R dst) {
		Set<R> out = outgoing.get(src);
		if (out == null) {
			outgoing.put(src, out = new HashSet<R>());
		}
		out.add(dst);

		Set<R> in = incoming.get(dst);
		if (in == null) {
			incoming.put(dst, in = new HashSet<R>());
		}
		in.add(src);
	}

	/**
	** Returns all sources that point to the given source.
	*/
	public Set<R> getIncoming(R src) {
		// FIXME NORM should really be immutable view
		return incoming.get(src);
	}

	/**
	** Returns a map of sources to their incoming sources.
	*/
	public Map<R, Set<R>> getIncoming() {
		// FIXME NORM should really be immutable view
		return incoming;
	}

	/**
	** Mark a data source as being in use.
	**
	** TODO maybe make this call inferScore() or something.
	*/
	public void useSource(R src) {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<R, L> getSources() {
		return in_use;
	}

}
