// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.ui;

import tags.proto.Query;
import tags.proto.QueryProcessor;
import tags.util.Maps.U2Map;
import tags.util.exec.MessageRejectedException;

import java.util.logging.Logger;

/**
** This class handles execution of {@link QueryProcessor}s and reports their
** progress and state, etc.
*/
public class QueryAgent<I, T, A, U, W, S, Z> {

	final protected Logger log;
	final protected QueryStateFormatter<T, A, W> fmt;

	public QueryAgent(Logger log, QueryStateFormatter<T, A, W> fmt) {
		this.log = log;
		this.fmt = fmt;
	}

	/**
	** Run until the first results are obtained, then run for {@code n} more
	** steps.
	*/
	public void runUntilAfter(QueryProcessor<I, T, A, U, W, S, Z> proc, int n) {
		// get some results
		while (proc.getResults() == null || proc.getResults().isEmpty()) {
			nextStep(proc);
		}
		showResults(proc.getResults(), proc.query);

		U2Map<A, A, W> res = proc.getResults();
		for (int i=0; i<n; ++i) {
			nextStep(proc);
			if (proc.getResults() == res) { continue; }
			res = proc.getResults();
			log.info(proc.query + " " + proc.getStatus() + " " + proc.getStats());
			showResults(res, proc.query);

			String [] lines = fmt.formatLookups(proc.routing.getCompletedLookups(), proc.naming.getAddressScheme().tagSet());
			for (String line: lines) { log.finest(line); }
		}
	}

	public void showResults(U2Map<A, A, W> res, Query<I, T> query) {
		log.info("Query " + query + " results: " + res.K0Map().size() + " doc, " + res.K1Map().size() + " idx");
		log.finest("doc: " + fmt.formatResults(res.K0Map()));
		log.finest("idx: " + fmt.formatResults(res.K1Map()));
	}

	public void nextStep(QueryProcessor<I, T, A, U, W, S, Z> proc) {
		try {
			proc.getMoreData();
			log.fine(proc.query + " " + proc.getStatus() + " " + proc.getStats());
		} catch (MessageRejectedException e) {
			String msg = e.getMessage();
			if (!msg.equals("bad timing") && !msg.substring(0,15).equals("invalid message")) {
				log.fine(e.getMessage());
			}
		}
		try { Thread.sleep(250); } catch (InterruptedException e) { }
	}

}
