// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.LayerService;
import tags.proto.Query;
import tags.proto.QueryProcessor;
import tags.util.MessageReceiver;
import tags.util.MessageRejectedException;

import tags.proto.MultiParts;
import tags.util.Maps;

import tags.proto.AddressScheme;
import tags.proto.DataSources;
import tags.proto.LocalIndex;
import tags.proto.FullIndex;
import tags.proto.Lookup;
import tags.util.Maps.U2Map;
import tags.util.Union.U2;
import tags.util.Arc;
import java.util.Set;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.HashMap;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public class Routing<T, A, W, S> extends LayerService<Query<?, T>, QueryProcessor<?, T, A, ?, W, S, ?>>
implements MessageReceiver<Routing.MSG_I> {

	public static enum MSG_I { REQ_MORE_DATA, RECV_SEED_H, RECV_ADDR_SCH }

	final protected IndexComposer<T, A, W, S> mod_idx_cmp;
	final protected LookupScorer<W, S> mod_lku_scr;

	final protected DataSources<A, LocalIndex<T, A, W>, S> source;
	final protected Map<A, Set<T>> lookup;

	protected FullIndex<T, A, W> index;

	public Routing(
		Query<?, T> query,
		QueryProcessor<?, T, A, ?, W, S, ?> proc,
		IndexComposer<T, A, W, S> mod_idx_cmp,
		LookupScorer<W, S> mod_lku_scr
	) {
		super(query, proc);
		if (mod_idx_cmp == null) { throw new NullPointerException(); }
		if (mod_lku_scr == null) { throw new NullPointerException(); }
		this.mod_idx_cmp = mod_idx_cmp;
		this.mod_lku_scr = mod_lku_scr;
		// TODO NOW
		this.source = null;
		this.lookup = null;
	}

	@Override public void recv(MSG_I msg) throws MessageRejectedException {
		switch (msg) {
		case REQ_MORE_DATA: // request more data, from the user
			break;
		case RECV_SEED_H: // receive seed indexes, from Contact
			break;
		case RECV_ADDR_SCH: // receive update to address scheme, from Naming
			break;
		}
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<A, LocalIndex<T, A, W>> getResults() {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** This is to be called whenever:
	**
	** - address scheme is updated
	** - we add a source
	*/
	public Map<A, Set<T>> getLookups(AddressScheme<T, A, W> scheme) {
		Map<A, Set<T>> lookups = new HashMap<A, Set<T>>();
		for (A idx: source.localMap().keySet()) {
			if (source.seedMap().containsKey(idx)) {
				// select all T in scheme
				lookups.put(idx, scheme.tagSet());
			} else {
				Set<T> tags = new HashSet<T>();
				for (A in_node: source.getIncoming(idx)) {
					LocalIndex<T, A, W> view = source.localMap().get(in_node);
					assert view.getIncomingHarcAttrMap(idx) != null;
					// find Set<T> that we reached A by
					for (T tag: view.getIncomingHarcAttrMap(idx).keySet()) {
						// for all T in Set<T>, select all "short" paths in layer_lo.getAddressScheme()
						tags.addAll(scheme.ancestorMap().K0Map().get(tag));
					}
				}
				lookups.put(idx, tags);
			}
		}
		return lookups;
	}

	public PriorityQueue<Lookup<T, A>> sortLookups(AddressScheme<T, A, W> scheme, Map<A, Set<T>> lookups) {
		Map<Lookup<T, A>, W> lku_score = new HashMap<Lookup<T, A>, W>();

		for (Map.Entry<A, Set<T>> en: lookups.entrySet()) {
			A idx = en.getKey();
			Set<T> tags = en.getValue();
			S idxs = source.scoreMap().get(idx);

			for (T tag: tags) {
				lku_score.put(Lookup.lookup(idx, tag), mod_lku_scr.getLookupScore(idxs, scheme.arcAttrMap().get(tag)));
			}
		}

		return mod_lku_scr.sortLookups(lku_score);
	}

	/**
	** DOCUMENT
	*/
	public FullIndex<T, A, W> composeIndex() {
		// iterates through all arcs present in every source
		U2Map<Arc<T, A>, Arc<T, A>, W> arc_map = Maps.uniteDisjoint(new HashMap<Arc<T, A>, W>(), new HashMap<Arc<T, A>, W>());
		for (U2<Arc<T, A>, Arc<T, A>> arc: Maps.domain(MultiParts.iterIndexArcMaps(source.localMap().values()))) {
			arc_map.put(arc, mod_idx_cmp.composeArc(source.localScoreMap(), arc));
		}

		return new FullIndex<T, A, W>(arc_map);
	}

	/**
	** Returns a map of results to their scores.
	**
	** TODO NORM arguably this could be in a separate module instead of just
	** "pick the most relevant tag".
	*/
	public Map<A, W> getResults(AddressScheme<T, A, W> scheme) {
		Map<A, W> results = new HashMap<A, W>();

		for (A dst: index.nodeSetD()) {
			// get most relevant tag which points to it
			Map<T, W> in_tag = index.getIncomingDarcAttrMap(dst);
			assert in_tag != null && !in_tag.isEmpty();
			T nearest = scheme.getMostRelevant(in_tag.keySet());

			W wgt = mod_lku_scr.getResultAttr(scheme.arcAttrMap().get(nearest), in_tag.get(nearest));
			results.put(dst, wgt);
		}

		for (A dst: index.nodeSetH()) {
			// get most relevant tag which points to it
			Map<T, W> in_tag = index.getIncomingHarcAttrMap(dst);
			assert in_tag != null && !in_tag.isEmpty();
			T nearest = scheme.getMostRelevant(in_tag.keySet());

			W wgt = mod_lku_scr.getResultAttr(scheme.arcAttrMap().get(nearest), in_tag.get(nearest));
			results.put(dst, wgt);
		}

		return results;
	}

}
