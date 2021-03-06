// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util.exec;

/**
** A receiver of messages.
**
** @param <M> Type of message
*/
public interface MessageReceiver<M> {

	public void recv(M message) throws MessageRejectedException;

}
