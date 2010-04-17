// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

/**
** An object that represents a query from some identity for some tag.
**
** @param <I> Type of identity
** @param <T> Type of tag
*/
public class Query<I, T> {

	final public I id;
	final public T tag;

	public Query(I id, T tag) {
		if (id == null || tag == null) { throw new NullPointerException(); }
		this.id = id;
		this.tag = tag;
	}

	public String toString() {
		return "[" + id + ":" + tag + "]";
	}

}
