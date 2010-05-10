// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.LayerService;
import tags.proto.QueryProcess;
import tags.proto.cont.Contact;
import tags.proto.name.Naming;

import tags.util.exec.MessageReceiver;
import tags.util.exec.MessageRejectedException;
import tags.util.exec.Services;
import tags.util.exec.TaskResult;
import tags.util.exec.TaskService;
import java.io.IOException;

import tags.proto.MultiParts;
import tags.proto.LocalViewFactory;
import tags.util.ScoreInferer;
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
import tags.util.ProxyMap;
import tags.util.MapQueue;
import tags.util.BaseMapQueue;
import java.util.Collection;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.EnumMap;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public class Routing<T, A, W, S>
extends LayerService<QueryProcess<?, T, A, ?, W, S, ?>, Routing.State, Routing.MRecv> {

	public enum State { NEW, AWAIT_SEEDS, AWAIT_ADDR_SCH, IDLE }
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
		QueryProcess<?, T, A, ?, W, S, ?> proc,
		IndexComposer<T, A, W, S> mod_idx_cmp,
		LookupScorer<W, S> mod_lku_scr,
		LocalViewFactory<A, LocalIndex<T, A, W>> view_fac,
		ScoreInferer<S> score_inf
	) {
		super("R", proc, State.NEW);
		if (mod_idx_cmp == null) { throw new NullPointerException(); }
		if (mod_lku_scr == null) { throw new NullPointerException(); }
		this.mod_idx_cmp = mod_idx_cmp;
		this.mod_lku_scr = mod_lku_scr;
		this.source = new DataSources<A, LocalIndex<T, A, W>, S>(view_fac, score_inf);
		this.completed = new HashMap<A, Set<T>>();
	}

	@Override public synchronized void recv(MRecv msg) throws MessageRejectedException {
		//super.recv(msg);
		switch (state) {
		case NEW:
			switch (msg) {
			case REQ_MORE_DATA:
				sendAtomic(State.AWAIT_SEEDS, Services.defer(proc.naming, Naming.MRecv.REQ_MORE_DATA));

				return;
			default: throw mismatchMsgRejEx(state, msg);
			}
		case AWAIT_SEEDS:
			switch (msg) {
			case RECV_SEED_H:
				// init data structures etc. reset everything
				source.setSeeds(proc.contact.getSeedIndexes());
				state = State.AWAIT_ADDR_SCH;

				return;
			default: throw mismatchMsgRejEx(state, msg);
			}
		case AWAIT_ADDR_SCH:
			switch (msg) {
			case RECV_ADDR_SCH:
				state = State.IDLE;

				// TODO HIGH this is a hack
				execute(new Runnable() {
					@Override public void run() {
						proc.log("runLookups thread started");
						runLookups();
						proc.log("runLookups thread exited successfully");
					}
				});

				return;
			default: throw mismatchMsgRejEx(state, msg);
			}
		case IDLE:
			AddressScheme<T, A, W> scheme = proc.naming.getAddressScheme();
			switch (msg) {
			case REQ_MORE_DATA:
				// OPT NORM this is really really really wasteful
				updateResults(scheme);

				Map<ActionChoice, W> choices = new EnumMap<ActionChoice, W>(ActionChoice.class);

				Map.Entry<A, W> idx_en = scheme.getMostRelevant(results.K1Map());
				if (idx_en != null) {
					A idx = idx_en.getKey();
					assert !source.getIncoming(idx).isEmpty();
					Collection<W> probs = scoreLookups(scheme, idx, getLookups(scheme, idx)).values();
					choices.put(ActionChoice.add_idx, mod_lku_scr.getPotential(probs));
				}
				if (!hasNothingToDo()) {
					choices.put(ActionChoice.con_lku, queue.peekValue());
				}
				if (scheme.isIncomplete()) {
					T tag = scheme.getIncomplete();
					Map<A, Set<T>> plku = new HashMap<A, Set<T>>();
					// OPT LOW use a Maps.fromKeys(keys, value) view instead
					for (A idx: source.localMap().keySet()) { plku.put(idx, Collections.singleton(tag)); }
					Collection<W> probs = scoreLookups(scheme, plku).values();
					choices.put(ActionChoice.add_tag, mod_lku_scr.getPotential(probs));
				}

				if (choices.isEmpty()) {
					proc.naming.recv(Naming.MRecv.REQ_MORE_DATA);
					return;
				}

				Map.Entry<ActionChoice, W> en = scheme.getMostRelevant(choices);
				//System.out.println(choices + " " + en);
				switch (en.getKey()) {
				case con_lku:
					// continue with lookups
					break;
				case add_idx:
					// add a new index
					A idx = idx_en.getKey();
					results.K1Map().remove(idx);
					// TODO HIGH probably limit the number of indexes that can be added before
					// receiving data back from the network
					//addDataSourceAndLookups(scheme, idx);
					break;
				case add_tag:
					proc.naming.recv(Naming.MRecv.REQ_MORE_DATA);
					break;
				}

				return;
			case RECV_ADDR_SCH:
				// update set of lookups
				addLookupsFromNewAddressScheme(scheme);

				return;
			default: throw mismatchMsgRejEx(state, msg);
			}
		}
	}

	public U2Map<A, A, W> getResults() {
		return results;
	}

	private enum ActionChoice { add_idx, con_lku, add_tag }

	private transient Map<A, Set<T>> _completed;
	public Map<A, Set<T>> getCompletedLookups() {
		if (_completed == null) {
			_completed = new ProxyMap<A, Set<T>, Set<T>>(completed) {
				@Override public Set<T> itemFor(Set<T> elem) {
					return Collections.<T>unmodifiableSet(elem);
				}
			};
		}
		return _completed;
	}

	public synchronized int countLookups() {
		int s = 0;
		for (Set<T> tag: completed.values()) {
			s += tag.size();
		}
		return s;
	}

	public int countResultsD() {
		return results == null? 0: results.K0Map().size();
	}

	public int countResultsH() {
		return results == null? 0: results.K1Map().size();
	}

	// TODO HIGH this is a major hack...
	protected Set<Lookup<T, A>> pending = new HashSet<Lookup<T, A>>();
	protected synchronized boolean hasNothingToDo() {
		return queue.isEmpty() && pending.isEmpty();
	}

	protected void runLookups() {
		TaskService<Lookup<T, A>, U2Map<A, A, W>, IOException> srv = proc.env.makeIndexService();

		try {
			do {
				synchronized (this) {
					while (pending.size() < proc.env.parallel_idx_lku && !queue.isEmpty()) {
						Lookup<T, A> lku = queue.remove();
						srv.submit(Services.newTask(lku));
						pending.add(lku);
					}
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

						boolean removed = pending.remove(lku);
						assert removed;
					}
				}

				Thread.sleep(proc.env.interval);
			} while (true);

		} catch (InterruptedException e) {
			throw new UnsupportedOperationException(e); // FIXME HIGH
		} catch (IOException e) {
			throw new RuntimeException(e); // FIXME HIGH
		} finally {
			srv.close();
		}
	}

	protected void addDataSourceAndLookups(AddressScheme<T, A, W> scheme, A addr) {
		proc.log("addDataSourceAndLookups: " + addr);
		// FIXME HIGH sync/race bug here, this method might be called twice with the same argument
		source.useSource(addr);
		submitLookupsToRunQueue(scoreLookups(scheme, Collections.singletonMap(addr, getLookups(scheme, addr))));
	}

	protected void addLookupsFromNewAddressScheme(AddressScheme<T, A, W> scheme) {
		proc.log("addLookupsFromNewAddressScheme: " + scheme.tagSet());
		Map<A, Set<T>> lookups = getLookups(scheme);
		synchronized (this) { Maps.multiMapRemoveAll(lookups, completed); }
		queue.clear();
		submitLookupsToRunQueue(scoreLookups(scheme, lookups));
	}

	protected synchronized void submitLookupsToRunQueue(Map<Lookup<T, A>, W> lku_score) {
		for (Map.Entry<Lookup<T, A>, W> en: lku_score.entrySet()) {
			Lookup<T, A> lku = en.getKey();
			// skip already running
			if (pending.contains(lku)) { continue; }
			// skip already completed
			Set<T> tags = completed.get(lku.idx);
			if (tags != null && tags.contains(lku.tag)) { continue; }
			//System.out.println("add " + lku + " to queue");
			//System.out.println("add " + lku + " to " + queue.map().keySet());
			queue.add(lku, en.getValue());
		}
	}

	protected synchronized void updateResults(AddressScheme<T, A, W> scheme) {
		if (rcache_v == true) { return; }
		source.calculateScores();
		index = composeIndex(); // FIXME HIGH - need to lock the local views whilst doing this
		// otherwise it might throw ConcurrentExecutionException
		results = makeResults(scheme);
		rcache_v = true;
	}

	/**
	** Get new lookups to do. This is to be called whenever:
	**
	** - address scheme is updated
	*/
	protected Map<A, Set<T>> getLookups(AddressScheme<T, A, W> scheme) {
		Map<A, Set<T>> lookups = new HashMap<A, Set<T>>();
		for (A idx: source.localMap().keySet()) {
			lookups.put(idx, source.seedMap().containsKey(idx)? new HashSet<T>(scheme.tagSet()): getLookups(scheme, idx));
		}
		return lookups;
	}

	/**
	** Get new lookups to do. This is to be called whenever:
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
		// seed tag has no ancestors; if idx's incoming tags consist only of the seed tag,
		// then the loop won't actually add the seed tag
		tags.add(scheme.seedTag());
		assert !tags.isEmpty();
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
			assert idxs != null;

			for (T tag: tags) {
				lku_score.put(Lookup.make(idx, tag), mod_lku_scr.getLookupScore(idxs, scheme.arcAttrMap().get(tag)));
			}
		}
		return lku_score;
	}

	/**
	** DOCUMENT
	*/
	protected Map<Lookup<T, A>, W> scoreLookups(AddressScheme<T, A, W> scheme, A idx, Set<T> tags) {
		Map<Lookup<T, A>, W> lku_score = new HashMap<Lookup<T, A>, W>();
		S idxs = source.inferScore(idx);
		for (T tag: tags) {
			lku_score.put(Lookup.make(idx, tag), mod_lku_scr.getLookupScore(idxs, scheme.arcAttrMap().get(tag)));
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
			// filter out indexes that are already in-use as a data source
			if (arc.isT1() && source.localMap().containsKey(arc.getT1().dst)) { continue; }
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
			Map.Entry<T, W> nearest = scheme.getMostRelevant(in_tag.keySet());
			// eg. if an update to the address scheme deleted those tags
			if (nearest == null) { continue; }

			W wgt = mod_lku_scr.getResultAttr(nearest.getValue(), in_tag.get(nearest.getKey()));
			res_d.put(dst, wgt);
		}

		for (A dst: index.nodeSetH()) {
			// get most relevant tag which points to it
			Map<T, W> in_tag = index.getIncomingHarcAttrMap(dst);
			assert in_tag != null && !in_tag.isEmpty();
			Map.Entry<T, W> nearest = scheme.getMostRelevant(in_tag.keySet());
			// eg. if an update to the address scheme deleted those tags
			if (nearest == null) { continue; }

			W wgt = mod_lku_scr.getResultAttr(nearest.getValue(), in_tag.get(nearest.getKey()));
			res_h.put(dst, wgt);
		}

		return Maps.uniteDisjoint(res_d, res_h);
	}

}
