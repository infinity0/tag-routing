// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
*/
public class Lookup<T, A> {

	final public A index;
	final public T tag;

	public Lookup(A index, T tag) {
		this.index = index;
		this.tag = tag;
	}

}
