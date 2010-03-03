// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.cont;

import tags.proto.LayerService;
import tags.proto.Query;
import tags.proto.QueryProcessor;
import tags.util.MessageReceiver;
import tags.util.MessageRejectedException;

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
public class Contact<I, A, S, Z> extends LayerService<Query<I, ?>, QueryProcessor<I, ?, A, ?, ?, S, Z>>
implements MessageReceiver<Contact.MSG_I> {

	public enum MSG_I { REQ_MORE_DATA }

	final protected PTableComposer<I, A, S, Z> mod_ptb_cmp;

	// TODO NORM maybe use a DataSources for this too...
	final protected MapX2<I, PTable<A, S>, Z> source;
	final protected PTable<A, S> table;

	final protected Iterable<Map<A, S>> src_score_g;
	final protected Iterable<Map<A, S>> src_score_h;

	public Contact(
		Query<I, ?> query,
		QueryProcessor<I, ?, A, ?, ?, S, Z> proc,
		PTableComposer<I, A, S, Z> mod_ptb_cmp
	) {
		super(query, proc);
		if (mod_ptb_cmp == null) { throw new NullPointerException(); }
		this.mod_ptb_cmp = mod_ptb_cmp;
		// TODO NOW
		this.source = null;
		this.table = null;
		this.src_score_g = MultiParts.iterTGraphs(source.MapV0().values());
		this.src_score_h = MultiParts.iterIndexes(source.MapV0().values());
	}

	@Override public synchronized void recv(MSG_I msg) throws MessageRejectedException {
		switch (msg) {
		case REQ_MORE_DATA: // request for more data, from Naming

			// if no data
			// - get data

			// otherwise,
			// - get more data (not worked out)
			// - ask user to supply different tag? (not worked out)
			throw new UnsupportedOperationException("not implemented");

			break;
		}
		assert false;
	}

	public Map<A, S> getSeedTGraphs() {
		return table.getTGraphs();
	}

	public Map<A, S> getSeedIndexes() {
		return table.getIndexes();
	}

	/**
	** Make a new {@link #table} from {@link #source}. To be called
	** whenever the latter changes.
	*/
	protected PTable<A, S> composePTable() {
		Map<A, S> g = new HashMap<A, S>(), h = new HashMap<A, S>();
		for (A addr: Maps.domain(src_score_g)) {
			g.put(addr, mod_ptb_cmp.composePTableGNode(source, addr));
		}
		for (A addr: Maps.domain(src_score_h)) {
			g.put(addr, mod_ptb_cmp.composePTableHNode(source, addr));
		}
		return new PTable<A, S>(g, h);
	}

}
