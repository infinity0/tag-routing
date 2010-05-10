// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.ui;

import tags.proto.Query;
import tags.proto.QueryProcess;
import tags.util.exec.MessageRejectedException;

import tags.util.BaseMapQueue;
import tags.util.MapQueue;
import tags.util.Maps.U2Map;
import java.util.Map;
import java.util.Collections;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.concurrent.ExecutionException;

/**
** This class handles execution of {@link QueryProcess}s and reports their
** progress and state, etc.
*/
public class QueryAgent<I, T, A, U, W, S, Z> {

	/** The {@link Logger} that output is sent to. */
	final public Logger log;
	/** The {@link QueryStateFormatter} used. */
	final public QueryStateFormatter<T, A, W> fmt;

	protected int interval = 250;

	public QueryAgent(Logger log, QueryStateFormatter<T, A, W> fmt) {
		this.log = log;
		this.fmt = fmt;
	}

	/**
	** Set the time interval between successive steps of the algorithm.
	*/
	public void setInterval(int interval) {
		if (interval < 10) {
			throw new IllegalArgumentException("interval must be >=10: " + interval);
		}
		this.interval = interval;
	}

	/**
	** @see #runUntilAfter(QueryProcess, int, ResultsReporter, int[])
	*/
	public void runUntilAfter(QueryProcess<I, T, A, U, W, S, Z> proc, int n) throws ExecutionException {
		runUntilAfter(proc, n, null, null);
	}

	/**
	** Run until the initial address scheme is obtained, then run for {@code n}
	** more steps.
	**
	** @param proc The process to run
	** @param n Number of steps to run after the first address scheme
	** @param report Reporter to send reports to
	** @param rsteps Steps at which to send reports
	*/
	public void runUntilAfter(
		QueryProcess<I, T, A, U, W, S, Z> proc, int n, ResultsReporter report, int[] rsteps
	) throws ExecutionException {
		// get some results
		while (proc.getResults() == null || proc.getResults().isEmpty()) {
			nextStep(proc);
		}
		showResults(proc.getResults(), proc);

		if (rsteps == null) {
			rsteps = new int[]{n-1};
		} else {
			Arrays.sort(rsteps);
		}

		U2Map<A, A, W> res = proc.getResults();
		for (int i=0; i<n; ++i) {
			nextStep(proc);

			// report
			if (report != null && Arrays.binarySearch(rsteps, i) >= 0) {
				report.addReport(prepareReport(proc, i));
			}

			// don't log same status twice
			if (proc.getResults() == res) { continue; }

			res = proc.getResults();
			log.info(proc + " " + proc.getStatus() + " " + proc.getStats());
			showResults(res, proc);

			try{
				String[] lines;
				log.finer("================");
				lines = fmt.formatAddressScheme(proc.naming.getAddressScheme());
				for (String line: lines) { log.finer(line); }
				log.finer("================");
				lines = fmt.formatLookups(proc.routing.getCompletedLookups(), proc.naming.getAddressScheme().tagSet());
				for (String line: lines) { log.finer(line); }
				log.finer("================");
			} catch (RuntimeException e) {
				// FIXME NORM
				// sometimes we get a ConcurrentModificationException, no time to fix right now
				System.err.println("ignoring RuntimeException: ");
				e.printStackTrace(System.err);
			}
		}
	}

	public void nextStep(QueryProcess<I, T, A, U, W, S, Z> proc) throws ExecutionException {
		try {
			proc.getMoreData();
			log.fine(proc + " " + proc.getStatus() + " " + proc.getStats());
		} catch (MessageRejectedException e) {
			String msg = e.getMessage();
			if (!msg.equals("bad timing") && !msg.substring(0,15).equals("invalid message")) {
				throw new ExecutionException("non-trivial message rejection", e);
			}
		}
		try { Thread.sleep(interval); } catch (InterruptedException e) { }
	}

	public void showResults(U2Map<A, A, W> res, Query<I, T> query) {
		log.info("Query " + query + " results: " + res.K0Map().size() + " doc, " + res.K1Map().size() + " idx");
		//log.finest("doc: " + fmt.formatResults(res.K0Map()));
		//log.finest("idx: " + fmt.formatResults(res.K1Map()));
	}

	public String prepareReport(QueryProcess<I, T, A, U, W, S, Z> proc, int step) {
		MapQueue<A, W> sres = sorted(proc.getResults().K0Map());
		StringBuilder report = new StringBuilder();
		report.append("# report at step ").append(step).append(":\n\n");
		for (String line: fmt.formatAddressScheme(proc.naming.getAddressScheme())) {
			report.append(line).append('\n');
		}
		report.append("\nresults (doc): ").append(sres).append("\n\n");
		return report.toString();
	}

	public MapQueue<A, W> sorted(Map<A, W> resmap) {
		MapQueue<A, W> sorted = new BaseMapQueue<A, W>(Collections.<W>reverseOrder(), false);
		sorted.addAll(resmap);
		return sorted;
	}

}
