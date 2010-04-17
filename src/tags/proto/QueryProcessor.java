// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.proto.cont.Contact;
import tags.proto.name.Naming;
import tags.proto.route.Routing;

import tags.proto.cont.PTableComposer;
import tags.proto.name.TGraphComposer;
import tags.proto.name.AddressSchemeBuilder;
import tags.proto.route.IndexComposer;
import tags.proto.route.LookupScorer;
import tags.util.ScoreInferer;

import tags.util.Maps.U2Map;

import tags.util.exec.MessageRejectedException;

/**
** DOCUMENT.
**
** @param <I> Type of identity
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
** @param <S> Type of score
** @param <Z> Type of identity-score
*/
public class QueryProcess<I, T, A, U, W, S, Z> extends Query<I, T> {

	final public QueryEnvironment<I, T, A, U, W, S, Z> env;

	final public Contact<I, A, S, Z> contact;
	final public Naming<T, A, U, W, S> naming;
	final public Routing<T, A, W, S> routing;

	protected int steps;

	public QueryProcess(
	  I id, T tag,
	  PTableComposer<I, A, S, Z> mod_ptb_cmp,
	  TGraphComposer<T, A, U, W, S> mod_tgr_cmp,
	  AddressSchemeBuilder<T, A, U, W> mod_asc_bld,
	  LocalViewFactory<A, LocalTGraph<T, A, U, W>> view_fac_g,
	  ScoreInferer<S> score_inf_g,
	  IndexComposer<T, A, W, S> mod_idx_cmp,
	  LookupScorer<W, S> mod_lku_scr,
	  LocalViewFactory<A, LocalIndex<T, A, W>> view_fac_h,
	  ScoreInferer<S> score_inf_h,
	  QueryEnvironment<I, T, A, U, W, S, Z> env
	) {
		super(id, tag);
		if (env == null) { throw new NullPointerException(); }
		this.env = env;
		this.contact = new Contact<I, A, S, Z>(this, mod_ptb_cmp);
		this.naming = new Naming<T, A, U, W, S>(this, mod_tgr_cmp, mod_asc_bld, view_fac_g, score_inf_g);
		this.routing = new Routing<T, A, W, S>(this, mod_idx_cmp, mod_lku_scr, view_fac_h, score_inf_h);
	}

	public void getMoreData() throws MessageRejectedException {
		routing.recv(Routing.MRecv.REQ_MORE_DATA);
		++steps;
	}

	public String getStats() {
		return "(" + steps + ")" +
		  " | G:" + naming.countSources() +
		  " | T:" + naming.countTagsInScheme() +
		  " | L:" + routing.countLookups() +
		  " | D:" + routing.countResultsD() +
		  " | H:" + routing.countResultsH() +
		  " |";
	}

	public String getStatus() {
		return contact.name + ":" + contact.getStatus() + " " + naming.name + ":" + naming.getStatus() + " " + routing.name + ":" + routing.getStatus();
	}

	public U2Map<A, A, W> getResults() {
		return routing.getResults();
	}

	public AddressScheme<T, A, W> getAddressScheme() {
		return naming.getAddressScheme();
	}

}
