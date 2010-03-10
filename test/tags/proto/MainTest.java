// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import junit.framework.TestCase;

import tags.proto.cont.*;
import tags.proto.name.*;
import tags.proto.route.*;
import tags.store.*;
import tags.util.*;
import tags.util.exec.*;

import java.util.concurrent.*;
import java.util.*;
import java.io.IOException;

public class MainTest extends TestCase {

	public static class SimpleQP<I, T, S> extends QueryProcessor<I, T, I, S, S, S, S> {
		public SimpleQP(
			Query<I, T> query,
			StoreControl<I, T, I, S, S, S, S> sctl,
			PTableComposer<I, I, S, S> mod_ptb_cmp,
			TGraphComposer<T, I, S, S, S> mod_tgr_cmp,
			AddressSchemeBuilder<T, I, S, S> mod_asc_bld,
			LocalViewFactory<I, LocalTGraph<T, I, S, S>> view_fac_g,
			ScoreInferer<S> score_inf_g,
			IndexComposer<T, I, S, S> mod_idx_cmp,
			LookupScorer<S, S> mod_lku_scr,
			LocalViewFactory<I, LocalIndex<T, I, S>> view_fac_h,
			ScoreInferer<S> score_inf_h,
			Executor exec
		) {
			super(query, sctl, mod_ptb_cmp, mod_tgr_cmp, mod_asc_bld, view_fac_g, score_inf_g, mod_idx_cmp, mod_lku_scr, view_fac_h, score_inf_h, exec);
		};
	}

	public static class DefaultQP extends SimpleQP<Long, String, Probability> {
		public DefaultQP(
			Query<Long, String> query,
			StoreControl<Long, String, Long, Probability, Probability, Probability, Probability> sctl,
			PTableComposer<Long, Long, Probability, Probability> mod_ptb_cmp,
			TGraphComposer<String, Long, Probability, Probability, Probability> mod_tgr_cmp,
			AddressSchemeBuilder<String, Long, Probability, Probability> mod_asc_bld,
			LocalViewFactory<Long, LocalTGraph<String, Long, Probability, Probability>> view_fac_g,
			ScoreInferer<Probability> score_inf_g,
			IndexComposer<String, Long, Probability, Probability> mod_idx_cmp,
			LookupScorer<Probability, Probability> mod_lku_scr,
			LocalViewFactory<Long, LocalIndex<String, Long, Probability>> view_fac_h,
			ScoreInferer<Probability> score_inf_h,
			Executor exec
		) {
			super(query, sctl, mod_ptb_cmp, mod_tgr_cmp, mod_asc_bld, view_fac_g, score_inf_g, mod_idx_cmp, mod_lku_scr, view_fac_h, score_inf_h, exec);
		};
	}

	public void testProbabilityQueryProcessor() {
		Executor exec = new ThreadPoolExecutor(
			0x40, 0x40, 1, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(),
			new ThreadPoolExecutor.CallerRunsPolicy()
		);
		FileStoreControl<Long, String, Long, Probability, Probability, Probability, Probability> sctl =
		new FileStoreControl<Long, String, Long, Probability, Probability, Probability, Probability>(".");
		StoreGenerator.sctl_gen_all(sctl);

		Query<Long, String> query = new Query<Long, String>(8028L, "aacs");

		DefaultQP proc = new DefaultQP(query, sctl,
			new ProtoPTableComposer<Long, Long>(),
			new ProbabilityEntropyTGraphComposer<String, Long>(),
			new ShortestPathAddressSchemeBuilder<String, Long, Probability, Probability, Probability>(new ProbabilityDistanceMetric()),
			LocalTGraph.<String, Long, Probability, Probability>getFactory(),
			new SPUProbabilityInferer(),
			new ProbabilityIndexComposer<String, Long>(),
			new ProbabilityLookupScorer(),
			LocalIndex.<String, Long, Probability>getFactory(),
			new SPUProbabilityInferer(),
			exec
		);

		while (proc.getResults() == null || proc.getResults().isEmpty()) {
			try {
				proc.getMoreData();
			} catch (MessageRejectedException e) {
				if (!e.getMessage().equals("bad timing")) { System.out.println(e); }
			}
			System.out.println("[ " + proc.contact.getStatus() + " | " + proc.naming.getStatus() + " | " + proc.routing.getStatus() + " ]");
			try { Thread.sleep(250); } catch (InterruptedException e) { }
		}

		System.out.println("Got " + proc.getResults().size() + " results for " + query + ":");
		System.out.println(proc.getResults());

		// TODO NOW do shit
	}

}
