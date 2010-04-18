// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.ui;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.Formatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;

/**
** Utility class for {@link Logger}s.
*/
public class Loggers {

	public static Logger makeConsoleShortLogger() {
		return makeConsoleShortLogger(Level.INFO);
	}

	public static Logger makeConsoleShortLogger(Level level) {
		Logger log = Logger.getAnonymousLogger();
		log.setUseParentHandlers(false);
		ConsoleHandler hd = new ConsoleHandler();
		hd.setFormatter(new Formatter() {
			@Override public synchronized String format(LogRecord record) {
				StringBuilder sb = new StringBuilder();
				long t = System.currentTimeMillis();
				sb.append(t/1000).append('.').append(String.format("%03d", t%1000)).append(" | ");
				sb.append(record.getLevel().getLocalizedName()).append(" | ");
				sb.append(formatMessage(record));
				sb.append('\n');
				return sb.toString();
			}
		});
		log.setLevel(level);
		hd.setLevel(level);
		log.addHandler(hd);
		return log;
	}

	private static transient Logger nullLogger;
	public static synchronized Logger getNullLogger() {
		if (nullLogger == null) {
			nullLogger = Logger.getAnonymousLogger();
			nullLogger.setUseParentHandlers(false);
		}
		return nullLogger;
	}

}
