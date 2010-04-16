// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.proto.Query;
import tags.store.StoreControl;
import java.util.concurrent.Executor;

import tags.proto.cont.PTableComposer;
import tags.proto.name.TGraphComposer;
import tags.proto.name.AddressSchemeBuilder;
import tags.proto.route.IndexComposer;
import tags.proto.route.LookupScorer;
import tags.util.ScoreInferer;

import tags.proto.cont.ProtoPTableComposer;
import tags.proto.name.ProbabilityDistanceMetric;
import tags.proto.name.ProbabilityEntropyTGraphComposer;
import tags.proto.name.ShortestPathAddressSchemeBuilder;
import tags.proto.route.ProbabilityIndexComposer;
import tags.proto.route.ProbabilityLookupScorer;
import tags.util.SPUProbabilityInferer;

import tags.util.Probability;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
** Utility class for {@link QueryProcessor}s.
*/
public class QueryProcessors {

	private QueryProcessors() { }

	/**
	** A {@link QueryProcessor} with one single score type, and one single
	** node type.
	**
	** @param <K> Type of node
	** @param <T> Type of tag
	** @param <S> Type of score
	*/
	public static class SimpleQP<K, T, S> extends QueryProcessor<K, T, K, S, S, S, S> {
		public SimpleQP(
			Query<K, T> query,
			StoreControl<K, T, K, S, S, S, S> sctl,
			PTableComposer<K, K, S, S> mod_ptb_cmp,
			TGraphComposer<T, K, S, S, S> mod_tgr_cmp,
			AddressSchemeBuilder<T, K, S, S> mod_asc_bld,
			LocalViewFactory<K, LocalTGraph<T, K, S, S>> view_fac_g,
			ScoreInferer<S> score_inf_g,
			IndexComposer<T, K, S, S> mod_idx_cmp,
			LookupScorer<S, S> mod_lku_scr,
			LocalViewFactory<K, LocalIndex<T, K, S>> view_fac_h,
			ScoreInferer<S> score_inf_h,
			Executor exec
		) {
			super(query, sctl, mod_ptb_cmp, mod_tgr_cmp, mod_asc_bld, view_fac_g, score_inf_g, mod_idx_cmp, mod_lku_scr, view_fac_h, score_inf_h, exec);
		};
	}

	/**
	** A {@link QueryProcessor} with one single node type, with {@link String}
	** tags and {@link Probability} scores.
	**
	** @param <K> Type of node
	*/
	public static class BasicQP<K> extends SimpleQP<K, String, Probability> {
		public BasicQP(
			Query<K, String> query,
			StoreControl<K, String, K, Probability, Probability, Probability, Probability> sctl,
			PTableComposer<K, K, Probability, Probability> mod_ptb_cmp,
			TGraphComposer<String, K, Probability, Probability, Probability> mod_tgr_cmp,
			AddressSchemeBuilder<String, K, Probability, Probability> mod_asc_bld,
			LocalViewFactory<K, LocalTGraph<String, K, Probability, Probability>> view_fac_g,
			ScoreInferer<Probability> score_inf_g,
			IndexComposer<String, K, Probability, Probability> mod_idx_cmp,
			LookupScorer<Probability, Probability> mod_lku_scr,
			LocalViewFactory<K, LocalIndex<String, K, Probability>> view_fac_h,
			ScoreInferer<Probability> score_inf_h,
			Executor exec
		) {
			super(query, sctl, mod_ptb_cmp, mod_tgr_cmp, mod_asc_bld, view_fac_g, score_inf_g, mod_idx_cmp, mod_lku_scr, view_fac_h, score_inf_h, exec);
		};
	}

	public static Executor makeDefaultExecutor() {
		return new ThreadPoolExecutor(
			0x40, 0x40, 1, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(),
			new ThreadPoolExecutor.CallerRunsPolicy()
		);
	}

	public static <K> BasicQP<K> makeProtoQP(Query<K, String> query, StoreControl<K, String, K, Probability, Probability, Probability, Probability> sctl, Executor exec) {
		return new BasicQP<K>(query, sctl,
			new ProtoPTableComposer<K, K>(),
			new ProbabilityEntropyTGraphComposer<String, K>(),
			new ShortestPathAddressSchemeBuilder<String, K, Probability, Probability, Probability>(new ProbabilityDistanceMetric()),
			LocalTGraph.<String, K, Probability, Probability>getFactory(),
			new SPUProbabilityInferer(),
			new ProbabilityIndexComposer<String, K>(),
			new ProbabilityLookupScorer(),
			LocalIndex.<String, K, Probability>getFactory(),
			new SPUProbabilityInferer(),
			exec
		);
	}

}
