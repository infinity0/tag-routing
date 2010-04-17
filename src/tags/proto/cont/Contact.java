// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.cont;

import tags.proto.LayerService;
import tags.proto.QueryProcess;
import tags.proto.name.Naming;
import tags.proto.route.Routing;

import tags.util.exec.MessageReceiver;
import tags.util.exec.MessageRejectedException;
import tags.util.exec.Services;
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
** OPT NORM abstract away from QueryProcess since this depends only on the
** identity and not any query tag, and therefore can be run continually in the
** background.
**
** @param <I> Type of identity
** @param <A> Type of address
** @param <S> Type of score
** @param <Z> Type of identity-score
*/
public class Contact<I, A, S, Z>
extends LayerService<QueryProcess<I, ?, A, ?, ?, S, Z>, Contact.State, Contact.MRecv> {

	public enum State { NEW, IDLE }
	public enum MRecv { REQ_MORE_DATA }

	final protected PTableComposer<I, A, S, Z> mod_ptb_cmp;

	// TODO NORM maybe use a DataSources for this too...
	final protected MapX2<I, PTable<A, S>, Z> source;

	volatile protected PTable<A, S> table;

	public Contact(
		QueryProcess<I, ?, A, ?, ?, S, Z> proc,
		PTableComposer<I, A, S, Z> mod_ptb_cmp
	) {
		super("C", proc, State.NEW);
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
				}
			}, State.IDLE,
			Services.defer(proc.routing, Routing.MRecv.RECV_SEED_H),
			Services.defer(proc.naming, Naming.MRecv.RECV_SEED_G));

			return;
		case IDLE:
			throw new MessageRejectedException("not implemented");
		}
	}

	public Map<A, S> getSeedTGraphs() {
		return table == null? null: table.getTGraphs();
	}

	public Map<A, S> getSeedIndexes() {
		return table == null? null: table.getIndexes();
	}

	protected void makePTable() {
		TaskService<I, PTable<A, S>, IOException> srv = proc.env.makePTableService();
		try {
			Map<I, Z> id_score = proc.env.getTrustedIDs(proc.id);

			for (I id: id_score.keySet()) { srv.submit(Services.newTask(id)); }
			do {
				while (srv.hasComplete()) {
					TaskResult<I, PTable<A, S>, IOException> res = srv.reclaim();
					if (res.getValue() == null) { throw new NullPointerException(); }
					source.putX2(res.getKey(), res.getValue(), id_score.get(res.getKey()));
				}

				Thread.sleep(proc.env.interval);
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
		// for (Map.Entry<I, PTable<A, S>> en: source.MapV0().entrySet()) { System.out.println(en); }
		for (A addr: Maps.domain(MultiParts.iterTGraphs(source.MapV0().values()))) {
			g.put(addr, mod_ptb_cmp.composePTableGNode(source, addr));
		}
		for (A addr: Maps.domain(MultiParts.iterIndexes(source.MapV0().values()))) {
			h.put(addr, mod_ptb_cmp.composePTableHNode(source, addr));
		}
		return new PTable<A, S>(g, h);
	}

}
