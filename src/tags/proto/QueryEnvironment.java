// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.store.StoreControl;
import tags.util.Probability;

import tags.util.Maps.U2Map;
import java.util.Map;

import tags.util.exec.TaskService;
import java.util.concurrent.Executor;
import java.io.IOException;

/**
** An environment for running {@link QueryProcess}es. This holds objects that
** should be useful for all processes running in this environment; eg. an
** {@link Executor} and a {@link StoreControl}.
**
** Generally, there will be one instance of this for each storage network that
** needs to be queried, per running application.
*/
public class QueryEnvironment<I, T, A, U, W, S, Z> {

	// interval for Thread.sleep in the control-loops in Contact/Naming/Routing
	final public int interval = 1000;

	// TODO NORM this is a hack, make a better way of doing this
	final public int parallel_idx_lku = 0x10;

	final public Executor exec;
	final public StoreControl<I, T, A, U, W, S, Z> sctl;

	public QueryEnvironment(Executor exec, StoreControl<I, T, A, U, W, S, Z> sctl) {
		this.exec = exec;
		this.sctl = sctl;
	}

	/**
	** Retrieves a set of inferred trusted identities from the social graph,
	** each mapped to their score rating.
	**
	** This method blocks until the operation is complete.
	**
	** TODO NORM this should really be a module in the Contact layer.
	*/
	public Map<I, Z> getTrustedIDs(I id) throws IOException {
		// TODO HIGH better implementation than this
		return sctl.getFriends(id);
	}

	public TaskService<I, PTable<A, S>, IOException> makePTableService() {
		// TODO HIGH better implementation than this
		return new tags.util.exec.UnthreadedTaskService<I, PTable<A, S>, IOException>() {
			@Override protected PTable<A, S> getResultFor(I id) throws IOException {
				return sctl.getPTable(id);
			}
		};
	}

	public TaskService<TGraph.Lookup<T, A>, U2Map<T, A, W>, IOException> makeTGraphService() {
		// TODO HIGH better implementation than this
		return new tags.util.exec.UnthreadedTaskService<TGraph.Lookup<T, A>, U2Map<T, A, W>, IOException>() {
			@Override protected U2Map<T, A, W> getResultFor(TGraph.Lookup<T, A> lku) throws IOException {
				return sctl.getTGraphOutgoing(lku.tgr, lku.tag);
			}
		};
	}

	public TaskService<TGraph.NodeLookup<T, A>, U, IOException> makeTGraphNodeService() {
		// TODO HIGH better implementation than this
		return new tags.util.exec.UnthreadedTaskService<TGraph.NodeLookup<T, A>, U, IOException>() {
			@Override protected U getResultFor(TGraph.NodeLookup<T, A> lku) throws IOException {
				return sctl.getTGraphNodeAttr(lku.tgr, lku.node);
			}
		};
	}

	public TaskService<Index.Lookup<T, A>, U2Map<A, A, W>, IOException> makeIndexService() {
		// TODO HIGH better implementation than this
		return new tags.util.exec.UnthreadedTaskService<Index.Lookup<T, A>, U2Map<A, A, W>, IOException>() {
			@Override protected U2Map<A, A, W> getResultFor(Index.Lookup<T, A> lku) throws IOException {
				return sctl.getIndexOutgoing(lku.idx, lku.tag);
			}
		};
	}

}
