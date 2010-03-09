// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util.exec;

/**
** A deferred message to be sent later.
**
** @param <M> Type of message
*/
public class DeferredMessage<M> {

	final public MessageReceiver<M> recv;
	final public M msg;

	public DeferredMessage(MessageReceiver<M> recv, M msg) {
		this.recv = recv;
		this.msg = msg;
	}

	public void send() throws MessageRejectedException {
		// TODO NORM make this sendable only once
		recv.recv(msg);
	}

}
