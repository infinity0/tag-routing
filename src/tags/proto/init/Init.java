// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.init;

import tags.proto.LayerService;
import tags.proto.Query;
import tags.store.StoreControl;
import tags.util.LayerInterfaceHi;
import tags.proto.name.Naming;

import tags.proto.MultiParts;
import tags.util.Maps;

import tags.proto.PTable;
import java.util.Map;
import java.util.HashMap;

/**
** DOCUMENT.
**
** OPTIMISE NORM abstract away from "Query" since this depends only on the
** identity and not any query tag, and therefore can be run continually in the
** background.
**
** @param <I> Type of identity
** @param <A> Type of address
** @param <S> Type of score
** @param <R> Type of identity-score
*/
public class Init<I, A, S, R> extends LayerService<Query<I, ?>, StoreControl<I, ?, A, ?, ?, S, R>> implements
LayerInterfaceHi<Integer, Naming<?, A, ?, ?, S>> {

	protected Naming<?, A, ?, ?, S> layer_hi;

	final protected PTableComposer<S, R> mod_ptb_cmp;

	final protected Map<PTable<A, S>, R> src_score;
	final protected PTable<A, S> table;

	final protected Iterable<Map<A, S>> src_score_g;
	final protected Iterable<Map<A, S>> src_score_h;

	public Init(
		Query<I, ?> query,
		StoreControl<I, ?, A, ?, ?, S, R> sctl,
		PTableComposer<S, R> mod_ptb_cmp
	) {
		super(query, sctl);
		this.mod_ptb_cmp = mod_ptb_cmp;
		// TODO NOW
		this.src_score = null;
		this.table = null;
		this.src_score_g = MultiParts.iterTGraphs(src_score.keySet());
		this.src_score_h = MultiParts.iterIndexes(src_score.keySet());
	}

	@Override public void setLayerHi(Naming<?, A, ?, ?, S> layer_hi) {
		this.layer_hi = layer_hi;
	}

	@Override public Integer request() {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<A, S> getSeedTGraphs() {
		return table.getTGraphs();
	}

	public Map<A, S> getSeedIndexes() {
		return table.getIndexes();
	}

	/**
	** Make a new {@link #table} from {@link #src_score}. To be called
	** whenever the latter changes.
	*/
	protected PTable<A, S> composePTable() {
		Map<A, S> g = new HashMap<A, S>(), h = new HashMap<A, S>();
		for (A addr: Maps.domain(src_score_g)) {
			g.put(addr, mod_ptb_cmp.composePTableNode(src_score, addr));
		}
		for (A addr: Maps.domain(src_score_h)) {
			g.put(addr, mod_ptb_cmp.composePTableNode(src_score, addr));
		}
		return new PTable<A, S>(g, h);
	}

}
