// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.LayerService;
import tags.proto.Query;
import tags.store.StoreControl;
import tags.util.LayerInterfaceLo;

import java.util.Map;

/**
** DOCUMENT.
**
** @param <A> Type of address
** @param <W> Type of arc-attribute
*/
public class Ranking<A, W> extends LayerService<Query<?, ?>, StoreControl<?, ?, ?, ?, ?, ?, ?>> implements
LayerInterfaceLo<Integer, Routing<?, A, W, ?>> {

	protected Routing<?, A, W, ?> layer_lo;

	final protected Map<A, W> result;

	public Ranking(
		Query<?, ?> query,
		StoreControl<?, ?, ?, ?, ?, ?, ?> sctl
	) {
		super(query, sctl);
		// TODO NOW
		this.result = null;
	}

	@Override public void setLayerLo(Routing<?, A, W, ?> layer_lo) {
		this.layer_lo = layer_lo;
	}

	@Override public void receive(Integer tkt) {
		throw new UnsupportedOperationException("not implemented");
	}

}
