// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.UnitService;
import tags.proto.Query;
import tags.store.StoreControl;

/**
** DOCUMENT.
**
** @param <Q> Type of query
** @param <S> Type of store control
*/
abstract public class LayerService<Q extends Query, S extends StoreControl> extends UnitService {

	final protected Q query;
	final protected S sctl;

	public LayerService(Q query, S sctl) {
		super(query.exec);
		this.query = query;
		this.sctl = sctl;
	}

}
