// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.init;

import tags.proto.Query;
import tags.proto.PTable;
import tags.proto.name.Naming;
import tags.store.StoreControl;
import tags.util.LayerInterfaceHi;
import tags.util.UnitService;

import java.util.Map;

/**
** DOCUMENT.
**
** @param <A> Type of address
** @param <S> Type of score
** @param <R> Type of identity-score
*/
public class Init<I, A, S, R> extends UnitService implements
LayerInterfaceHi<Integer, Naming<?, A, ?, ?, S>> {

	final protected Query<I, ?> query;
	final protected StoreControl<I, ?, A, ?, ?, S, R> sctl;
	protected Naming<?, A, ?, ?, S> layer_hi;

	final protected PTableComposer<S, R> mod_ptb_cmp;

	final protected Map<PTable<A, S>, R> source;
	final protected PTable<A, S> ptable;

	public Init(
		Query<I, ?> query,
		StoreControl<I, ?, A, ?, ?, S, R> sctl,
		PTableComposer<S, R> mod_ptb_cmp
	) {
		super(query.exec);
		this.query = query;
		this.sctl = sctl;
		this.mod_ptb_cmp = mod_ptb_cmp;
		// TODO NOW
		this.source = null;
		this.ptable = null;
	}

	@Override public void setLayerHi(Naming<?, A, ?, ?, S> layer_hi) {
		this.layer_hi = layer_hi;
	}

	@Override public Integer request() {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<A, S> getSeedTGraphs() {
		return ptable.getTGraphs();
	}

	public Map<A, S> getSeedIndexes() {
		return ptable.getIndexes();
	}

	protected PTable<A, S> composePTable() {
		throw new UnsupportedOperationException("not implemented");
	}

}
