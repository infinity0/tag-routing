// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import java.util.concurrent.Executor;

/**
** DOCUMENT.
**
** @param <I> Type of identity
** @param <T> Type of tag
*/
public class Query<I, T> {

	final public Executor exec;

	final public I seed_id;
	final public T seed_tag;

	public Query(Executor exec, I seed_id, T seed_tag) {
		this.exec = exec;
		this.seed_id = seed_id;
		this.seed_tag = seed_tag;
	}

}
