// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.init;

import tags.proto.PTable;
import tags.proto.name.Naming;
import tags.store.StoreControl;
import tags.util.LayerInterfaceHi;

import java.util.Map;

/**
** DOCUMENT.
**
** @param <A> Type of address
** @param <S> Type of score
** @param <R> Type of identity-score
*/
public class Init<A, S, R> implements
LayerInterfaceHi<Integer, Naming<?, A, ?, ?, S>> {

	final protected StoreControl<?, ?, A, ?, ?, S, R> sctl;

	protected Naming<?, A, ?, ?, S> layer_hi;

	final protected PTableComposer<S, R> mod_ptb_cmp;

	// TODO NORM maybe have <I> as well
	final protected Map<PTable<A, S>, R> source;

	public Init(
		StoreControl<?, ?, A, ?, ?, S, R> sctl,
		PTableComposer<S, R> mod_ptb_cmp
	) {
		this.sctl = sctl;
		this.mod_ptb_cmp = mod_ptb_cmp;
		// TODO NOW
		this.source = null;
	}

	@Override public void setLayerHi(Naming<?, A, ?, ?, S> layer_hi) {
		this.layer_hi = layer_hi;
	}

	@Override public Integer request() {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<A, S> getSeedTGraphs() {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<A, S> getSeedIndexes() {
		throw new UnsupportedOperationException("not implemented");
	}

}
