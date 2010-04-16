// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import junit.framework.TestCase;

import tags.proto.QueryProcessors.*;
import tags.proto.cont.*;
import tags.proto.name.*;
import tags.proto.route.*;
import tags.store.*;
import tags.util.*;
import tags.util.exec.*;
import tags.util.Maps.U2Map;

import java.util.concurrent.*;
import java.util.*;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Formatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;

public class MainTest extends TestCase {

	final protected static boolean verbose = Boolean.getBoolean("test.verbose");
	final protected static boolean extensive = Boolean.getBoolean("test.extensive");

	final protected Logger LOG;

	public MainTest() {
		Logger LOG = Logger.getAnonymousLogger();
		LOG.setUseParentHandlers(false);
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
		LOG.addHandler(hd);
		if (verbose) {
			LOG.setLevel(Level.ALL);
			hd.setLevel(Level.ALL);
		}
		this.LOG = LOG;
	}

	public void testProbabilityQueryProcessor() throws Throwable {
		if (!extensive) { return; }

		Executor exec = QueryProcessors.makeDefaultExecutor();
		RAMStoreControl<Long, String, Long, Probability, Probability, Probability, Probability> sctl = new
		RAMStoreControl<Long, String, Long, Probability, Probability, Probability, Probability>();

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
		LOG.info("Test data initialised with " + sctl.getSummary());

		LOG.info("----");
		for (long id: new long[]{8028L, 8032L, 8036L, 8040L, 8044L}) {
		//for (long id: new long[]{8044L}) {

			BasicQP<Long> proc = QueryProcessors.makeProtoQP(new Query<Long, String>(id, "aacs"), sctl, exec);
			LOG.info("Starting query " + proc.query);

			runUntilAfter(proc, 16);

			// test whether the results actually match
			int d = sctl.map_tag.get(proc.query.tag).size();
			int r = proc.getResults().K0Map().size();

			Set<Long> doc = new HashSet<Long>(sctl.map_tag.get(proc.query.tag));
			doc.retainAll(proc.getResults().K0Map().keySet());
			int x = doc.size();

			LOG.info(
				proc.naming.countSources() + " tgr * " +
				proc.naming.countTagsInScheme() + " tag, " +
				proc.routing.countLookups() + " lku: " +
				r + " res, " + d + " real (" + x + " match)");

			// fail if less than half the results actually match
			assertTrue(x<<1 > r);
			LOG.info("----");
		}

	}

	public void runUntilAfter(QueryProcessor<Long, String, Long, Probability, Probability, Probability, Probability> proc, int n) {

		// get some results
		while (proc.getResults() == null || proc.getResults().isEmpty()) {
			nextStep(proc);
		}
		showResults(proc.getResults(), proc.query);

		U2Map<Long, Long, Probability> res = proc.getResults();
		for (int i=0; i<n; ++i) {
			nextStep(proc);
			if (proc.getResults() == res) { continue; }
			res = proc.getResults();
			showResults(res, proc.query);
		}

	}

	public void showResults(U2Map<Long, Long, Probability> res, Query<Long, String> query) {
		LOG.info("Query " + query + " results: " + res.K0Map().size() + " doc, " + res.K1Map().size() + " idx");
		LOG.finest("doc: " + outputMap(res.K0Map()));
		LOG.finest("idx: " + outputMap(res.K1Map()));
	}

	public String outputMap(Map<Long, Probability> map) {
		StringBuilder s = new StringBuilder();
		s.append("{ ");
		for (Map.Entry<Long, Probability> en: map.entrySet()) {
			s.append("(").append(en.getKey()).append(':').append(en.getValue().toString().substring(0, 6)).append(") ");
		}
		s.append('}');
		return s.toString();
	}

	public void nextStep(QueryProcessor<Long, String, Long, Probability, Probability, Probability, Probability> proc) {
		try {
			proc.getMoreData();
		} catch (MessageRejectedException e) {
			if (!e.getMessage().equals("bad timing")) {
				LOG.fine(e.getMessage());
			}
		}
		LOG.fine("[ " + proc.contact.getStatus() + " | " + proc.naming.getStatus() + " | " + proc.routing.getStatus() + " ]");
		try { Thread.sleep(250); } catch (InterruptedException e) { }
	}

}
