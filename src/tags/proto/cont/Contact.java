// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.cont;

import tags.proto.LayerService;
import tags.proto.Query;
import tags.store.StoreControl;
import tags.util.LayerInterfaceHi;
import tags.proto.name.Naming;

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
public class Contact<I, A, S, Z> extends LayerService<Query<I, ?>, StoreControl<I, ?, A, ?, ?, S, Z>> implements
LayerInterfaceHi<Integer, Naming<?, A, ?, ?, S>> {

	protected Naming<?, A, ?, ?, S> layer_hi;

	final protected PTableComposer<I, A, S, Z> mod_ptb_cmp;

	// TODO NORM maybe use a DataSources for this too...
	final protected MapX2<I, PTable<A, S>, Z> source;
	final protected PTable<A, S> table;

	final protected Iterable<Map<A, S>> src_score_g;
	final protected Iterable<Map<A, S>> src_score_h;

	public Contact(
		Query<I, ?> query,
		StoreControl<I, ?, A, ?, ?, S, Z> sctl,
		PTableComposer<I, A, S, Z> mod_ptb_cmp
	) {
		super(query, sctl);
		this.mod_ptb_cmp = mod_ptb_cmp;
		// TODO NOW
		this.source = null;
		this.table = null;
		this.src_score_g = MultiParts.iterTGraphs(source.MapV0().values());
		this.src_score_h = MultiParts.iterIndexes(source.MapV0().values());
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
