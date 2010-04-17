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
** @param <Q> Type of query process
** @param <S> Type of state
** @param <M> Type of message
*/
abstract public class LayerService<Q extends QueryProcess, S extends Enum<S>, M>
extends UnitService<S> implements MessageReceiver<M> {

	final public String name;
	final protected Q proc;

	public LayerService(String name, Q proc, S state) {
		super(state, proc.env.exec);
		this.name = name;
		this.proc = proc;
	}

	public LayerService(Q proc, S state) {
		this("?", proc, state);
	}

	@Override public synchronized void recv(M msg) throws MessageRejectedException {
		if (isActive()) { throw new MessageRejectedException("bad timing"); }
	}

	protected static <S, M> MessageRejectedException mismatchMsgRejEx(S state, M msg) {
		return new MessageRejectedException("invalid message " + msg + " for state " + state);
	}

	public String getStatus() {
		return "[" + (last_ex == null? (active?'A':'I'): 'E') + "|" + state.ordinal() + "|" + completed + "]";
	}

}
