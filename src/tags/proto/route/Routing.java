// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.LayerService;
import tags.proto.Query;
import tags.proto.QueryProcessor;
import tags.proto.LocalViewFactory;
import tags.util.ScoreInferer;

import tags.util.exec.MessageReceiver;
import tags.util.exec.MessageRejectedException;
import tags.util.exec.Tasks;
import tags.util.exec.TaskResult;
import tags.util.exec.TaskService;
import java.io.IOException;

import tags.proto.MultiParts;
import tags.util.Maps;
import java.util.Collections;

import tags.proto.AddressScheme;
import tags.proto.DataSources;
import tags.proto.LocalIndex;
import tags.proto.FullIndex;
import tags.proto.Index.Lookup;
import tags.util.Maps.U2Map;
import tags.util.Union.U2;
import tags.util.Arc;
import tags.util.MapQueue;
import tags.util.BaseMapQueue;
import java.util.Set;
import java.util.Map;
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
public class Routing<T, A, W, S>
extends LayerService<Query<?, T>, QueryProcessor<?, T, A, ?, W, S, ?>, Routing.State, Routing.MRecv> {

	public enum State { NEW, AWAIT_SEEDS, IDLE }
	public enum MRecv { REQ_MORE_DATA, RECV_SEED_H, RECV_ADDR_SCH }

	final protected IndexComposer<T, A, W, S> mod_idx_cmp;
	final protected LookupScorer<W, S> mod_lku_scr;

	final protected DataSources<A, LocalIndex<T, A, W>, S> source;
	final protected Map<A, Set<T>> completed;

	final protected MapQueue<Lookup<T, A>, W> queue = new BaseMapQueue<Lookup<T, A>, W>(Collections.<W>reverseOrder(), true);

	protected FullIndex<T, A, W> index;
	volatile protected U2Map<A, A, W> results;
	protected boolean rcache_v; // accesses to this field need to be synchronized(Routing.this)

	public Routing(
		Query<?, T> query,
		QueryProcessor<?, T, A, ?, W, S, ?> proc,
		IndexComposer<T, A, W, S> mod_idx_cmp,
		LookupScorer<W, S> mod_lku_scr,
		LocalViewFactory<A, LocalIndex<T, A, W>> view_fac,
		ScoreInferer<S> score_inf
	) {
		super(query, proc);
		if (mod_idx_cmp == null) { throw new NullPointerException(); }
		if (mod_lku_scr == null) { throw new NullPointerException(); }
		this.mod_idx_cmp = mod_idx_cmp;
		this.mod_lku_scr = mod_lku_scr;
		this.source = new DataSources<A, LocalIndex<T, A, W>, S>(view_fac, score_inf);
		this.completed = new HashMap<A, Set<T>>();
	}

	@Override public synchronized void recv(MRecv msg) throws MessageRejectedException {
		super.recv(msg);
		switch (msg) {
		case REQ_MORE_DATA: // request more data, from the user

			// if no seeds, or no indexes to add, or some other heuristic
			// - pass request to naming layer

			// otherwise,
			// - complete some more lookups, or
			// - add an index as a data source

			//throw new UnsupportedOperationException("not implemented");

			break;
		case RECV_SEED_H: // receive seed indexes, from Contact

			// init data structures etc. reset everything.
			source.setSeeds(proc.contact.getSeedIndexes());
			//throw new UnsupportedOperationException("not implemented");

			break;
		case RECV_ADDR_SCH: // receive update to address scheme, from Naming

			// - update set of lookups
			// OPT HIGH only update things that need to be updated
			//throw new UnsupportedOperationException("not implemented");

			break;
		}
		assert false;
	}

	public U2Map<A, A, W> getResults() {
		return results;
	}

	protected void getMoreData(AddressScheme<T, A, W> scheme) {
		// OPT NORM this is really really really wasteful
		updateResults(scheme);
		Map.Entry<A, W> idx_en = scheme.getMostRelevant(results.K1Map());

		// TODO HIGH need to review this later; the algorithm decided ad-hoc and
		// without any forethought

		if (queue.isEmpty()) {
			addDataSourceAndLookups(scheme, idx_en.getKey());

		} else {
			W lku_s = queue.peekValue();
			if (scheme.comparator().compare(idx_en.getValue(), lku_s) > 0) {
				addDataSourceAndLookups(scheme, idx_en.getKey());
			}
		}
	}

	protected void runLookups() {
		TaskService<Lookup<T, A>, U2Map<A, A, W>, IOException> srv = proc.newIndexService();
		int pending = 0;

		try {
			do {
				while (pending < proc.parallel_idx_lku && !queue.isEmpty()) {
					srv.submit(Tasks.newTask(queue.remove()));
					++pending;
				}

				while (srv.hasComplete()) {
					TaskResult<Lookup<T, A>, U2Map<A, A, W>, IOException> res = srv.reclaim();
					Lookup<T, A> lku = res.getKey();
					// FIXME NORM RACE this might need synchronization. maybe we can get away with it;
					// Doing It Properly would either require use of ConcurrentHashMap in DataSources
					// or adding a shit load of synchronized() {} to this class...
					LocalIndex<T, A, W> view = source.localMap().get(lku.idx);

					synchronized (this) {
						rcache_v = false;
						view.setOutgoingT(lku.tag, res.getValue());

						// update completed lookups
						Set<T> tags = completed.get(lku.idx);
						if (tags == null) { completed.put(lku.idx, tags = new HashSet<T>()); }
						tags.add(lku.tag);
					}
					--pending;
				}

				Thread.sleep(proc.interval);
			} while (true);

		} catch (InterruptedException e) {
			// FIXME HIGH
		} catch (IOException e) {
			// FIXME HIGH
		} finally {
			srv.close();
		}
	}

	protected void addDataSourceAndLookups(AddressScheme<T, A, W> scheme, A addr) {
		source.useSource(addr);
		Map<Lookup<T, A>, W> lku_score = scoreLookups(scheme, Collections.singletonMap(addr, getLookups(scheme, addr)));
		for (Map.Entry<Lookup<T, A>, W> en: lku_score.entrySet()) { queue.add(en.getKey(), en.getValue()); }
	}

	protected void addLookupsFromNewAddressScheme(AddressScheme<T, A, W> scheme) {
		Map<A, Set<T>> lookups = getLookups(scheme);
		synchronized (this) { Maps.multiMapRemoveAll(lookups, completed); }
		Map<Lookup<T, A>, W> lku_score = scoreLookups(scheme, lookups);
		queue.clear();
		for (Map.Entry<Lookup<T, A>, W> en: lku_score.entrySet()) { queue.add(en.getKey(), en.getValue()); }
	}

	protected synchronized void updateResults(AddressScheme<T, A, W> scheme) {
		if (rcache_v == true) { return; }
		source.calculateScores();
		index = composeIndex();
		results = makeResults(scheme);
		rcache_v = true;
	}

	/**
	** This is to be called whenever:
	**
	** - address scheme is updated
	*/
	protected Map<A, Set<T>> getLookups(AddressScheme<T, A, W> scheme) {
		Map<A, Set<T>> lookups = new HashMap<A, Set<T>>();
		for (A idx: source.localMap().keySet()) {
			lookups.put(idx, source.seedMap().containsKey(idx)? scheme.tagSet(): getLookups(scheme, idx));
		}
		return lookups;
	}

	/**
	** This is to be called whenever:
	**
	** - we add a source
	*/
	protected Set<T> getLookups(AddressScheme<T, A, W> scheme, A idx) {
		Set<T> tags = new HashSet<T>();
		for (A in_node: source.getIncoming(idx)) {
			LocalIndex<T, A, W> view = source.localMap().get(in_node);
			assert view.getIncomingHarcAttrMap(idx) != null;
			// find all tags that we reached A by
			for (T tag: view.getIncomingHarcAttrMap(idx).keySet()) {
				// for each tag, select all "short" paths to that tag in scheme
				tags.addAll(scheme.ancestorMap().K0Map().get(tag));
			}
		}
		return tags;
	}

	/**
	** DOCUMENT
	*/
	protected Map<Lookup<T, A>, W> scoreLookups(AddressScheme<T, A, W> scheme, Map<A, Set<T>> lookups) {
		Map<Lookup<T, A>, W> lku_score = new HashMap<Lookup<T, A>, W>();

		for (Map.Entry<A, Set<T>> en: lookups.entrySet()) {
			A idx = en.getKey();
			Set<T> tags = en.getValue();
			S idxs = source.scoreMap().get(idx);

			for (T tag: tags) {
				lku_score.put(Lookup.make(idx, tag), mod_lku_scr.getLookupScore(idxs, scheme.arcAttrMap().get(tag)));
			}
		}
		return lku_score;
	}

	/**
	** DOCUMENT
	*/
	protected FullIndex<T, A, W> composeIndex() {
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
	protected U2Map<A, A, W> makeResults(AddressScheme<T, A, W> scheme) {
		Map<A, W> res_d = new HashMap<A, W>();
		Map<A, W> res_h = new HashMap<A, W>();

		for (A dst: index.nodeSetD()) {
			// get most relevant tag which points to it
			Map<T, W> in_tag = index.getIncomingDarcAttrMap(dst);
			assert in_tag != null && !in_tag.isEmpty();
			T nearest = scheme.getMostRelevant(in_tag.keySet());
			// FIXME HIGH nearest could be null if an update to the address scheme deleted all tags

			W wgt = mod_lku_scr.getResultAttr(scheme.arcAttrMap().get(nearest), in_tag.get(nearest));
			res_d.put(dst, wgt);
		}

		for (A dst: index.nodeSetH()) {
			// TODO HIGH filter out indexes that are in-use as a data source

			// get most relevant tag which points to it
			Map<T, W> in_tag = index.getIncomingHarcAttrMap(dst);
			assert in_tag != null && !in_tag.isEmpty();
			T nearest = scheme.getMostRelevant(in_tag.keySet());
			// FIXME HIGH nearest could be null if an update to the address scheme deleted all tags

			W wgt = mod_lku_scr.getResultAttr(scheme.arcAttrMap().get(nearest), in_tag.get(nearest));
			res_h.put(dst, wgt);
		}

		return Maps.uniteDisjoint(res_d, res_h);
	}

}
