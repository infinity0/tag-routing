// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

/**
** DOCUMENT.
**
** @param <I> Type of identity
** @param <T> Type of tag
*/
public class Query<I, T> {

	final public I seed_id;
	final public T seed_tag;

	public Query(I seed_id, T seed_tag) {
		if (seed_id == null || seed_tag == null) { throw new NullPointerException(); }
		this.seed_id = seed_id;
		this.seed_tag = seed_tag;
	}

	public String toString() {
		return "[" + seed_id + ":" + seed_tag + "]";
	}

}
