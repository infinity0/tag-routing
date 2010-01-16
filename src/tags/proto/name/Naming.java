// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.proto.LocalTGraph;
import tags.proto.TGraph;
import tags.proto.AddressScheme;
import tags.proto.route.Routing;
import tags.proto.init.Init;
import tags.store.StoreControl;
import tags.util.LayerInterfaceHi;
import tags.util.LayerInterfaceLo;

import java.util.Map;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public class Naming<T, A, U, W, S> implements
LayerInterfaceHi<Integer, Routing<T, A, W, S>>,
LayerInterfaceLo<Integer, Init<A, S, ?>> {

	final protected StoreControl<?, T, A, U, W, S, ?> sctl;

	protected Routing<T, A, W, S> layer_hi;
	protected Init<A, S, ?> layer_lo;

	final protected DistanceMetric<?, U, W> mod_dmtr;
	final protected TGraphComposer<U, W, S> mod_tgr_cmp;
	final protected TGraphScorer<U, W, S> mod_tgr_scr;

	final protected Map<A, LocalTGraph<T, U, W>> source;
	final protected Map<A, S> score;
	final protected TGraph<T, U, W> graph;
	final protected AddressScheme<T> scheme;

	public Naming(
		StoreControl<?, T, A, U, W, S, ?> sctl,
		DistanceMetric<?, U, W> mod_dmtr,
		TGraphComposer<U, W, S> mod_tgr_cmp,
		TGraphScorer<U, W, S> mod_tgr_scr
	) {
		this.sctl = sctl;
		this.mod_dmtr = mod_dmtr;
		this.mod_tgr_cmp = mod_tgr_cmp;
		this.mod_tgr_scr = mod_tgr_scr;
		// TODO NOW
		this.source = null;
		this.score = null;
		this.graph = null;
		this.scheme = null;
	}

	@Override public void setLayerHi(Routing<T, A, W, S> layer_hi) {
		this.layer_hi = layer_hi;
	}

	@Override public Integer request() {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override public void setLayerLo(Init<A, S, ?> layer_lo) {
		this.layer_lo = layer_lo;
	}

	@Override public void receive(Integer tkt) {
		throw new UnsupportedOperationException("not implemented");
	}

	public TGraph<T, U, W> getCompositeTGraph() {
		throw new UnsupportedOperationException("not implemented");
	}

	public AddressScheme<T> getAddressScheme() {
		throw new UnsupportedOperationException("not implemented");
	}

}
