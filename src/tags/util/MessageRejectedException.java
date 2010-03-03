// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.io.IOException;

/**
** DOCUMENT.
*/
public class MessageRejectedException extends IOException {

	final private static long serialVersionUID = 8688384884411841888L;

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
