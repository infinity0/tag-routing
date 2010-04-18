// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags;

import tags.proto.QueryProcess;
import tags.proto.LocalViewFactory;
import tags.proto.LocalTGraph;
import tags.proto.LocalIndex;
import tags.proto.cont.PTableComposer;
import tags.proto.name.TGraphComposer;
import tags.proto.name.AddressSchemeBuilder;
import tags.proto.route.IndexComposer;
import tags.proto.route.LookupScorer;
import tags.util.ScoreInferer;

import tags.proto.QueryEnvironment;
import tags.store.StoreControl;
import java.util.concurrent.Executor;

import tags.ui.QueryAgent;
import tags.ui.QueryStateFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

import tags.util.Probability;

import tags.proto.cont.ProtoPTableComposer;
import tags.proto.name.ProbabilityDistanceMetric;
import tags.proto.name.ProbabilityEntropyTGraphComposer;
import tags.proto.name.ShortestPathAddressSchemeBuilder;
import tags.proto.route.ProbabilityIndexComposer;
import tags.proto.route.ProbabilityLookupScorer;
import tags.util.SPUProbabilityInferer;

import tags.ui.QueryStateTextFormatter;
import tags.ui.Loggers;

/**
** Utility class for simplifying the types of various query objects.
**
** The classes named {@code SimpleXXX} consolidate all attribute types into a
** single type, and all node address types into a single type, leaving 3 type
** parameters rather than 7.
**
** The classes named {@code BasicXXX} define the attribute type to be {@link
** Probability} and the tag type to be {@link String}, leaving a single type
** parameter for the user to define, ie. the node address type.
**
** TODO NORM perhaps redesign the whole {@code QueryX} suite of classes to
** instead be interfaces?
*/
public class QueryTypes {

	private QueryTypes() { }

	/**
	** A {@link QueryProcess} with a single score type, and a single node type.
	**
	** @param <K> Type of node address
	** @param <T> Type of tag
	** @param <S> Type of attribute
	*/
	public static class SimpleProcess<K, T, S> extends QueryProcess<K, T, K, S, S, S, S> {
		public SimpleProcess(
		  K id, T tag,
		  PTableComposer<K, K, S, S> mod_ptb_cmp,
		  TGraphComposer<T, K, S, S, S> mod_tgr_cmp,
		  AddressSchemeBuilder<T, K, S, S> mod_asc_bld,
		  LocalViewFactory<K, LocalTGraph<T, K, S, S>> view_fac_g,
		  ScoreInferer<S> score_inf_g,
		  IndexComposer<T, K, S, S> mod_idx_cmp,
		  LookupScorer<S, S> mod_lku_scr,
		  LocalViewFactory<K, LocalIndex<T, K, S>> view_fac_h,
		  ScoreInferer<S> score_inf_h,
		  QueryEnvironment<K, T, K, S, S, S, S> env
		) {
			super(id, tag, mod_ptb_cmp,
			  mod_tgr_cmp, mod_asc_bld, view_fac_g, score_inf_g,
			  mod_idx_cmp, mod_lku_scr, view_fac_h, score_inf_h,
			  env);
		};
	}

	/**
	** A {@link QueryEnvironment} with a single score type, and a single node type.
	**
	** @param <K> Type of node address
	** @param <T> Type of tag
	** @param <S> Type of attribute
	*/
	public static class SimpleEnvironment<K, T, S> extends QueryEnvironment<K, T, K, S, S, S, S> {
		public SimpleEnvironment(Executor exec, StoreControl<K, T, K, S, S, S, S> sctl) {
			super(exec, sctl);
		}
	}

	/**
	** A {@link QueryAgent} with a single score type, and a single node type.
	**
	** @param <K> Type of node address
	** @param <T> Type of tag
	** @param <S> Type of attribute
	*/
	public static class SimpleAgent<K, T, S> extends QueryAgent<K, T, K, S, S, S, S> {
		public SimpleAgent(Logger log, QueryStateFormatter<T, K, S> fmt) {
			super(log, fmt);
		}
	}

	/**
	** A {@link QueryProcess} with a single node type, {@link String} tags and
	** {@link Probability} attributes.
	**
	** @param <K> Type of node address
	*/
	public static class BasicProcess<K> extends SimpleProcess<K, String, Probability> {
		public BasicProcess(
		  K id, String tag,
		  PTableComposer<K, K, Probability, Probability> mod_ptb_cmp,
		  TGraphComposer<String, K, Probability, Probability, Probability> mod_tgr_cmp,
		  AddressSchemeBuilder<String, K, Probability, Probability> mod_asc_bld,
		  LocalViewFactory<K, LocalTGraph<String, K, Probability, Probability>> view_fac_g,
		  ScoreInferer<Probability> score_inf_g,
		  IndexComposer<String, K, Probability, Probability> mod_idx_cmp,
		  LookupScorer<Probability, Probability> mod_lku_scr,
		  LocalViewFactory<K, LocalIndex<String, K, Probability>> view_fac_h,
		  ScoreInferer<Probability> score_inf_h,
		  QueryEnvironment<K, String, K, Probability, Probability, Probability, Probability> env
		) {
			super(id, tag, mod_ptb_cmp,
			  mod_tgr_cmp, mod_asc_bld, view_fac_g, score_inf_g,
			  mod_idx_cmp, mod_lku_scr, view_fac_h, score_inf_h,
			  env);
		};
	}

	/**
	** A {@link QueryEnvironment} with a single node type, {@link String} tags
	** and {@link Probability} attributes.
	**
	** @param <K> Type of node address
	*/
	public static class BasicEnvironment<K> extends SimpleEnvironment<K, String, Probability> {
		public BasicEnvironment(Executor exec, StoreControl<K, String, K, Probability, Probability, Probability, Probability> sctl) {
			super(exec, sctl);
			this.highscore_hack = Probability.MAX_VALUE;
		}
	}

	/**
	** A {@link QueryAgent} with a single node type, {@link String} tags and
	** {@link Probability} attributes.
	**
	** @param <K> Type of node address
	*/
	public static class BasicAgent<K> extends SimpleAgent<K, String, Probability> {
		public BasicAgent(Logger log, QueryStateFormatter<String, K, Probability> fmt) {
			super(log, fmt);
		}
	}

	public static <K> BasicProcess<K> makeProtoProcess(
	  K id, String tag,
	  QueryEnvironment<K, String, K, Probability, Probability, Probability, Probability> env
	) {
		return new BasicProcess<K>(
		  id, tag,
		  new ProtoPTableComposer<K, K>(),
		  new ProbabilityEntropyTGraphComposer<String, K>(),
		  new ShortestPathAddressSchemeBuilder<String, K, Probability, Probability, Probability>(new ProbabilityDistanceMetric()),
		  LocalTGraph.<String, K, Probability, Probability>getFactory(),
		  new SPUProbabilityInferer(),
		  new ProbabilityIndexComposer<String, K>(),
		  new ProbabilityLookupScorer(),
		  LocalIndex.<String, K, Probability>getFactory(),
		  new SPUProbabilityInferer(),
		  env
		);
	}

	public static <K> BasicEnvironment<K> makeProtoEnvironment(
	  StoreControl<K, String, K, Probability, Probability, Probability, Probability> sctl
	) {
		return new BasicEnvironment<K>(QueryEnvironment.makeDefaultExecutor(), sctl);
	}

	public static <K> BasicAgent<K> makeProtoAgent(
	  Logger log, QueryStateFormatter<String, K, Probability> fmt
	) {
		return new BasicAgent<K>(log, fmt);
	}

	public static <K> BasicAgent<K> makeProtoAgent(Level level) {
		return new BasicAgent<K>(Loggers.makeConsoleShortLogger(level), new QueryStateTextFormatter<String, K, Probability>());
	}

}
