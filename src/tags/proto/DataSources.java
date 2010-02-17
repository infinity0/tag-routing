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

	final protected LocalViewFactory<R, L> view_fac;

	public DataSources(LocalViewFactory<R, L> view_fac) {
		this.view_fac = view_fac;
	}

	/**
	** Returns the set of seed sources, each mapped to its local view.
	*/
	public Map<R, L> getSeeds() {
		// FIXME NORM should really be immutable view
		return seeds;
	}

	/**
	** Set outgoing arcs for the given source.
	*/
	public void setOutgoing(R src, Set<R> out_node) {
		Set<R> out = outgoing.get(src);
		if (out == null) {
			outgoing.put(src, out = new HashSet<R>());
		}
		out.addAll(out_node);
		// FIXME HIGH need to create entries in both incoming AND outgoing

		for (R dst: out_node) {
			Set<R> in = incoming.get(dst);
			if (in == null) {
				incoming.put(dst, in = new HashSet<R>());
			}
			in.add(src);
		}
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
	**
	** @return The blank local view that was created
	** @throws IllegalArgumentException if {@code src} is not a known source
	*/
	public L useSource(R src) {
		if (!outgoing.containsKey(src)) {
			throw new IllegalArgumentException("unknown source");
		}
		L view = view_fac.createLocalView(src, this);
		in_use.put(src, view);
		// TODO NORM bunch of other stuff needed too, probably
		return view;
	}

	public Map<R, L> getSources() {
		return in_use;
	}

}
