// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.store.StoreControl;
import tags.proto.cont.Contact;
import tags.proto.name.Naming;
import tags.proto.route.Routing;
import tags.util.exec.Task;
import tags.util.exec.TaskResult;
import tags.util.exec.TaskService;
import tags.util.exec.MessageRejectedException;
import java.util.concurrent.Executor;
import java.io.IOException;

import tags.proto.cont.PTableComposer;
import tags.proto.name.TGraphComposer;
import tags.proto.name.AddressSchemeBuilder;
import tags.proto.route.IndexComposer;
import tags.proto.route.LookupScorer;
import tags.util.ScoreInferer;

import tags.util.Maps.U2Map;
import java.util.Map;

/**
** DOCUMENT.
**
** @param <I> Type of identity
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
** @param <S> Type of score
** @param <Z> Type of identity-score
*/
public class QueryProcessor<I, T, A, U, W, S, Z> {

	final public Executor exec;
	final public Query<I, T> query;
	final public StoreControl<I, T, A, U, W, S, Z> sctl;

	final public Contact<I, A, S, Z> contact;
	final public Naming<T, A, U, W, S> naming;
	final public Routing<T, A, W, S> routing;

	// interval for Thread.sleep in the control-loops in Contact/Naming/Routing
	final public int interval = 1000;

	// TODO NORM this is a hack, make a better way of doing this
	final public int parallel_idx_lku = 0x10;

	public QueryProcessor(
		Query<I, T> query,
		StoreControl<I, T, A, U, W, S, Z> sctl,
		PTableComposer<I, A, S, Z> mod_ptb_cmp,
		TGraphComposer<T, A, U, W, S> mod_tgr_cmp,
		AddressSchemeBuilder<T, A, U, W> mod_asc_bld,
		LocalViewFactory<A, LocalTGraph<T, A, U, W>> view_fac_g,
		ScoreInferer<S> score_inf_g,
		IndexComposer<T, A, W, S> mod_idx_cmp,
		LookupScorer<W, S> mod_lku_scr,
		LocalViewFactory<A, LocalIndex<T, A, W>> view_fac_h,
		ScoreInferer<S> score_inf_h,
		Executor exec
	) {
		if (exec == null || sctl == null || query == null) { throw new NullPointerException(); }
		this.exec = exec;
		this.sctl = sctl;
		this.query = query;
		// TODO NOW
		this.contact = new Contact<I, A, S, Z>(query, this, mod_ptb_cmp);
		this.naming = new Naming<T, A, U, W, S>(query, this, mod_tgr_cmp, mod_asc_bld, view_fac_g, score_inf_g);
		this.routing = new Routing<T, A, W, S>(query, this, mod_idx_cmp, mod_lku_scr, view_fac_h, score_inf_h);
	}

	public void getMoreData() {
		try {
			routing.recv(Routing.MRecv.REQ_MORE_DATA);
		} catch (MessageRejectedException e) {
			// swallow it
		}
	}

	public U2Map<A, A, W> getResults() {
		return routing.getResults();
	}

	/**
	** Retrieves a set of inferred trusted identities from the social graph,
	** each mapped to their score rating.
	**
	** This method blocks until the operation is complete.
	**
	** TODO NORM this should really be a module in the Contact layer.
	*/
	public Map<I, Z> getTrustedIDs() throws IOException {
		// TODO HIGH better implementation than this
		return sctl.getFriends(query.seed_id);
	}

	public TaskService<I, PTable<A, S>, IOException> newPTableService() {
		// TODO HIGH better implementation than this
		return new tags.util.exec.UnthreadedTaskService<I, PTable<A, S>, IOException>() {
			@Override protected PTable<A, S> getResultFor(I id) throws IOException {
				return sctl.getPTable(id);
			}
		};
	}

	public TaskService<TGraph.Lookup<T, A>, U2Map<T, A, W>, IOException> newTGraphService() {
		// TODO HIGH better implementation than this
		return new tags.util.exec.UnthreadedTaskService<TGraph.Lookup<T, A>, U2Map<T, A, W>, IOException>() {
			@Override protected U2Map<T, A, W> getResultFor(TGraph.Lookup<T, A> lku) throws IOException {
				return sctl.getTGraphOutgoing(lku.tgr, lku.tag);
			}
		};
	}

	public TaskService<TGraph.NodeLookup<T, A>, U, IOException> newTGraphNodeService() {
		// TODO HIGH better implementation than this
		return new tags.util.exec.UnthreadedTaskService<TGraph.NodeLookup<T, A>, U, IOException>() {
			@Override protected U getResultFor(TGraph.NodeLookup<T, A> lku) throws IOException {
				return sctl.getTGraphNodeAttr(lku.tgr, lku.node);
			}
		};
	}

	public TaskService<Index.Lookup<T, A>, U2Map<A, A, W>, IOException> newIndexService() {
		// TODO HIGH better implementation than this
		return new tags.util.exec.UnthreadedTaskService<Index.Lookup<T, A>, U2Map<A, A, W>, IOException>() {
			@Override protected U2Map<A, A, W> getResultFor(Index.Lookup<T, A> lku) throws IOException {
				return sctl.getIndexOutgoing(lku.idx, lku.tag);
			}
		};
	}

}
