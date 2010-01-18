// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.Query;
import tags.proto.LocalIndex;
import tags.proto.name.Naming;
import tags.store.StoreControl;
import tags.util.LayerInterfaceHi;
import tags.util.LayerInterfaceLo;
import tags.util.UnitService;

import java.util.Set;
import java.util.Map;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public class Routing<T, A, W, S> extends UnitService implements
LayerInterfaceHi<Integer, Ranking<A, W>>,
LayerInterfaceLo<Integer, Naming<T, A, ?, W, S>> {

	final protected Query<?, T> query;
	final protected StoreControl<?, T, A, ?, W, S, ?> sctl;
	protected Ranking<A, W> layer_hi;
	protected Naming<T, A, ?, W, S> layer_lo;

	final protected IndexScorer<W, S> mod_idx_scr;

	final protected Map<A, LocalIndex<T, A, W>> source;
	final protected Map<A, S> score;
	final protected Map<A, Set<T>> lookup;
	final protected Map<A, LocalIndex<T, A, W>> result;

	public Routing(
		Query<?, T> query,
		StoreControl<?, T, A, ?, W, S, ?> sctl,
		IndexScorer<W, S> mod_idx_scr
	) {
		super(query.exec);
		this.query = query;
		this.sctl = sctl;
		this.mod_idx_scr = mod_idx_scr;
		// TODO NOW
		this.source = null;
		this.score = null;
		this.lookup = null;
		this.result = null;
	}

	@Override public void setLayerHi(Ranking<A, W> layer_hi) {
		this.layer_hi = layer_hi;
	}

	@Override public Integer request() {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override public void setLayerLo(Naming<T, A, ?, W, S> layer_lo) {
		this.layer_lo = layer_lo;
	}

	@Override public void receive(Integer tkt) {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<A, LocalIndex<T, A, W>> getResults() {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<A, S> getIndexScores() {
		throw new UnsupportedOperationException("not implemented");
	}

}
