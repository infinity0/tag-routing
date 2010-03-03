// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.store.StoreControl;
import tags.proto.cont.Contact;
import tags.proto.name.Naming;
import tags.proto.route.Routing;
import java.util.concurrent.Executor;

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
public class QueryProcessor<I, T, A, U, W, S, Z> {

	final public Executor exec;
	final public Query<I, T> query;
	final public StoreControl<I, T, A, U, W, S, Z> sctl;

	final public Contact<I, A, S, Z> contact;
	final public Naming<T, A, U, W, S> naming;
	final public Routing<T, A, W, S> routing;

	public QueryProcessor(Executor exec, Query<I, T> query, StoreControl<I, T, A, U, W, S, Z> sctl) {
		if (exec == null || sctl == null || query == null) { throw new NullPointerException(); }
		this.exec = exec;
		this.sctl = sctl;
		this.query = query;
		// TODO NOW
		this.contact = null;
		this.naming = null;
		this.routing = null;
	}

}
