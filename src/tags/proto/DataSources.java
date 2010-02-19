// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.ScoreInferer;
import tags.util.Maps;

import tags.util.Maps.MapX2;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

/**
** Graph of remote data sources that point to each other. DOCUMENT.
**
** @param <R> Type of remote address
** @param <L> Type of local view
** @param <S> Type of score
*/
public class DataSources<R, L, S> {

	final protected Map<R, S> seed_score = new HashMap<R, S>();

	final protected Map<R, Set<R>> outgoing = new HashMap<R, Set<R>>();
	final protected Map<R, Set<R>> incoming = new HashMap<R, Set<R>>();

	final protected Map<R, L> local = new HashMap<R, L>();
	final protected Map<R, S> score = new HashMap<R, S>();
	final protected MapX2<R, L, S> local_score = Maps.convoluteStrict(local, score, Maps.BaseMapX2.Inclusion.SUB1SUP0);

	final protected LocalViewFactory<R, L> view_fac;
	final protected ScoreInferer<S> score_inf;

	public DataSources(LocalViewFactory<R, L> view_fac, ScoreInferer<S> score_inf) {
		this.view_fac = view_fac;
		this.score_inf = score_inf;
	}

	/**
	** Returns a map view of seeds to their scores
	*/
	public Map<R, S> seedMap() {
		// FIXME NORM should really be immutable view
		return seed_score;
	}

	/**
	** Returns a map view of sources to their local views.
	*/
	public Map<R, L> localMap() {
		// FIXME NORM should really be immutable view
		return local;
	}

	/**
	** Returns a map view of sources to their scores.
	*/
	public Map<R, S> scoreMap() {
		// FIXME NORM should really be immutable view
		return score;
	}

	/**
	** Returns a map view of local views to their scores.
	*/
	public MapX2<R, L, S> localScoreMap() {
		// FIXME NORM should really be immutable view
		return local_score;
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
	** Set outgoing arcs for the given source.
	**
	** '''NOTE''': this method must be called whenever outgoing arcs are loaded
	** into the local view of the given source.
	*/
	public void setOutgoing(R src, Set<R> out_node) {
		Set<R> out = outgoing.get(src);
		if (out == null) {
			outgoing.put(src, out = new HashSet<R>());
			incoming.put(src, new HashSet<R>());
		}
		out.addAll(out_node);

		for (R dst: out_node) {
			Set<R> in = incoming.get(dst);
			if (in == null) {
				outgoing.put(dst, new HashSet<R>());
				incoming.put(dst, in = new HashSet<R>());
			}
			in.add(src);
		}
	}

	public void setSeeds(Map<R, S> seed_score) {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Mark a data source as being in use, and return a blank local view for
	** it.
	**
	** TODO maybe make this call score_inf.inferScore() or something.
	**
	** @return The empty local view that was created
	** @throws IllegalArgumentException if {@code src} is not a known source
	*/
	public L useSource(R src) {
		if (!outgoing.containsKey(src)) {
			throw new IllegalArgumentException("unknown source");
		}
		L view = view_fac.createLocalView(src, this);
		local.put(src, view);
		// TODO HIGH bunch of other stuff needed too, probably
		return view;
	}

	/**
	** Calculates the score for each in-use remote source.
	*/
	public void calculateScores() {
		// OPT NORM could somehow do this incrementally instead of re-calculating entire thing
		score.clear();
		for (R addr: local.keySet()) {
			score.put(addr, score_inf.inferScore(incoming, seed_score, addr));
		}
	}

}
