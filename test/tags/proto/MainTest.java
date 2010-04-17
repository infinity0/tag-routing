// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import junit.framework.TestCase;

import tags.proto.QueryProcesses.*;
import tags.proto.cont.*;
import tags.proto.name.*;
import tags.proto.route.*;
import tags.store.*;
import tags.ui.*;
import tags.util.*;
import tags.util.exec.*;
import tags.util.Maps.U2Map;

import java.util.concurrent.*;
import java.util.*;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.Formatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;

public class MainTest extends TestCase {

	final protected static boolean verbose = Boolean.getBoolean("test.verbose");
	final protected static boolean extensive = Boolean.getBoolean("test.extensive");

	final protected Logger log;

	public MainTest() {
		log = Logger.getAnonymousLogger();
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
		if (verbose) {
			log.setLevel(Level.ALL);
			hd.setLevel(Level.ALL);
		}
		log.addHandler(hd);
	}

	public void testProbabilityQueryProcessor() throws Throwable {
		if (!extensive) { return; }

		RAMStoreControl<Long, String, Long, Probability, Probability, Probability, Probability> sctl = new
		RAMStoreControl<Long, String, Long, Probability, Probability, Probability, Probability>();

		QueryEnvironment<Long, String, Long, Probability, Probability, Probability, Probability> env = new
		QueryEnvironment<Long, String, Long, Probability, Probability, Probability, Probability>(
		  QueryProcesses.makeDefaultExecutor(), sctl
		);

		try {
			Class<?> gen = Class.forName("tags.store.StoreGenerator");
			java.lang.reflect.Method method = gen.getMethod("sctl_gen_all", RAMStoreControl.class);
			try {
				method.invoke(null, sctl);
			} catch (java.lang.reflect.InvocationTargetException e) {
				throw e.getCause();
			}
		} catch (ClassNotFoundException e) {
			fail("Test data not generated; generate with `ant regen-data`.");
			return;
		}
		log.info("Test data initialised with " + sctl.getSummary());

		QueryAgent<Long, String, Long, Probability, Probability, Probability, Probability> run = new
		QueryAgent<Long, String, Long, Probability, Probability, Probability, Probability>(log,
		  new QueryStateTextFormatter<String, Long, Probability>());

		log.info("----");
		for (long id: new long[]{8028L, 8032L, 8036L, 8040L, 8044L}) {

			BasicQP<Long> proc = QueryProcesses.makeProtoQP(id, "aacs", env);
			log.info("Starting query " + proc);

			run.runUntilAfter(proc, 16);

			// test whether the results actually match
			int d = sctl.map_tag.get(proc.tag).size();
			int r = proc.getResults().K0Map().size();

			Set<Long> doc = new HashSet<Long>(sctl.map_tag.get(proc.tag));
			doc.retainAll(proc.getResults().K0Map().keySet());
			int x = doc.size();

			log.info(
				proc.naming.countSources() + " tgr * " +
				proc.naming.countTagsInScheme() + " tag, " +
				proc.routing.countLookups() + " lku: " +
				r + " res, " + d + " real (" + x + " match)");

			// fail if less than half the results actually match
			assertTrue(x<<1 > r);
			log.info("----");
		}

	}

}
