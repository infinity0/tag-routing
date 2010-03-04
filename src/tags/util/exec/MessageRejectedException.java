// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util.exec;

import java.io.IOException;

/**
** An exception thrown to indicate rejection of a message.
*/
public class MessageRejectedException extends IOException {

	final private static long serialVersionUID = 8662663023464374721L;

	public MessageRejectedException() { }

	public MessageRejectedException(String message, Throwable cause) {
		super(message, cause);
	}

	public MessageRejectedException(String message) {
		super(message);
	}

	public MessageRejectedException(Throwable cause) {
		super(cause);
	}

}
