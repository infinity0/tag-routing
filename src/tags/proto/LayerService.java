// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.exec.UnitService;
import tags.proto.Query;
import tags.store.StoreControl;

/**
** DOCUMENT.
**
** @param <Q> Type of query
** @param <P> Type of query processor
*/
abstract public class LayerService<Q extends Query, P extends QueryProcessor> extends UnitService {

	final protected Q query;
	final protected P proc;

	public LayerService(Q query, P proc) {
		super(proc.exec);
		this.query = query;
		this.proc = proc;
	}

}
