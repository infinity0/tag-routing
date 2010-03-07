// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.cont;

import tags.proto.LayerService;
import tags.proto.Query;
import tags.proto.QueryProcessor;
import tags.proto.name.Naming;
import tags.proto.route.Routing;

import tags.util.exec.MessageReceiver;
import tags.util.exec.MessageRejectedException;
import tags.util.exec.Tasks;
import tags.util.exec.TaskResult;
import tags.util.exec.TaskService;
import java.io.IOException;

import tags.proto.MultiParts;
import tags.util.Maps;

import tags.proto.PTable;
import tags.util.Maps.MapX2;
import java.util.Map;
import java.util.HashMap;

/**
** DOCUMENT.
**
** OPT NORM abstract away from "Query" since this depends only on the
** identity and not any query tag, and therefore can be run continually in the
** background.
**
** @param <I> Type of identity
** @param <A> Type of address
** @param <S> Type of score
** @param <Z> Type of identity-score
*/
public class Contact<I, A, S, Z>
extends LayerService<Query<I, ?>, QueryProcessor<I, ?, A, ?, ?, S, Z>, Contact.State, Contact.MRecv> {

	public enum State { NEW, IDLE }
	public enum MRecv { REQ_MORE_DATA }

	final protected PTableComposer<I, A, S, Z> mod_ptb_cmp;

	// TODO NORM maybe use a DataSources for this too...
	final protected MapX2<I, PTable<A, S>, Z> source;

	volatile protected PTable<A, S> table;

	public Contact(
		Query<I, ?> query,
		QueryProcessor<I, ?, A, ?, ?, S, Z> proc,
		PTableComposer<I, A, S, Z> mod_ptb_cmp
	) {
		super(query, proc, State.NEW);
		if (mod_ptb_cmp == null) { throw new NullPointerException(); }
		this.mod_ptb_cmp = mod_ptb_cmp;
		this.source = Maps.convoluteStrict(new HashMap<I, PTable<A, S>>(), new HashMap<I, Z>(), Maps.BaseMapX2.Inclusion.EQUAL);
	}

	@Override public synchronized void recv(MRecv msg) throws MessageRejectedException {
		super.recv(msg);
		switch (state) {
		case NEW:
			execute(new Runnable() {
				@Override public void run() {
					makePTable();
					try {
						proc.naming.recv(Naming.MRecv.RECV_SEED_G);
						proc.routing.recv(Routing.MRecv.RECV_SEED_H);

					} catch (MessageRejectedException e) {
						throw new RuntimeException(e); // FIXME HIGH
					}
				}
			}, State.IDLE);
			return;
		case IDLE:
			throw new MessageRejectedException("not implemented");
		}
	}

	public Map<A, S> getSeedTGraphs() {
		return table.getTGraphs();
	}

	public Map<A, S> getSeedIndexes() {
		return table.getIndexes();
	}

	protected void makePTable() {
		Map<I, Z> id_score = proc.getTrustedIDs();

		TaskService<I, PTable<A, S>, IOException> srv = proc.newPTableService();
		try {
			for (I id: id_score.keySet()) { srv.submit(Tasks.newTask(id)); }
			do {
				while (srv.hasComplete()) {
					TaskResult<I, PTable<A, S>, IOException> res = srv.reclaim();
					source.putX2(res.getKey(), res.getValue(), id_score.get(res.getKey()));
				}

				Thread.sleep(proc.interval);
			} while (srv.hasPending());

		} catch (InterruptedException e) {
			throw new UnsupportedOperationException(e); // FIXME HIGH
		} catch (IOException e) {
			throw new RuntimeException(e); // FIXME HIGH
		} finally {
			srv.close();
		}

		table = composePTable();
	}

	/**
	** Make a new {@link #table} from {@link #source}. To be called
	** whenever the latter changes.
	*/
	protected PTable<A, S> composePTable() {
		Map<A, S> g = new HashMap<A, S>(), h = new HashMap<A, S>();
		for (A addr: Maps.domain(MultiParts.iterTGraphs(source.MapV0().values()))) {
			g.put(addr, mod_ptb_cmp.composePTableGNode(source, addr));
		}
		for (A addr: Maps.domain(MultiParts.iterIndexes(source.MapV0().values()))) {
			g.put(addr, mod_ptb_cmp.composePTableHNode(source, addr));
		}
		return new PTable<A, S>(g, h);
	}

}
