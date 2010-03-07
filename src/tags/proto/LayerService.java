// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.proto.Query;
import tags.store.StoreControl;
import tags.util.exec.UnitService;
import tags.util.exec.MessageReceiver;
import tags.util.exec.MessageRejectedException;

/**
** DOCUMENT.
**
** @param <Q> Type of query
** @param <P> Type of query processor
** @param <S> Type of state
** @param <M> Type of message
*/
abstract public class LayerService<Q extends Query, P extends QueryProcessor, S, M>
extends UnitService<S> implements MessageReceiver<M> {

	final protected Q query;
	final protected P proc;

	public LayerService(Q query, P proc) {
		super(proc.exec);
		this.query = query;
		this.proc = proc;
	}

	@Override public synchronized void recv(M msg) throws MessageRejectedException {
		if (isActive()) { throw new MessageRejectedException("bad timing"); }
	}

}
