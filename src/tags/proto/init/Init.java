// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.init;

import tags.proto.PTable;
import tags.store.StoreControl;

import java.util.Map;

/**
** DOCUMENT.
**
** @param <A> Type of address
** @param <S> Type of score
** @param <R> Type of identity-score
*/
public class Init<A, S, R> {

	final protected StoreControl<?, ?, A, ?, ?, S, R> sctl;
	final protected PTableComposer<S, R> mod_ptb_cmp;

	// TODO NORM maybe have <I> as well
	final protected Map<PTable<A, S>, R> source;

	public Init(
		StoreControl<?, ?, A, ?, ?, S, R> sctl,
		PTableComposer<S, R> mod_ptb_cmp
	) {
		this.sctl = sctl;
		this.mod_ptb_cmp = mod_ptb_cmp;
		// TODO NOW
		this.source = null;
	}

}
