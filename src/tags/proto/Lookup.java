// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Tuple.X2;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
*/
public class Lookup<T, A> {

	final public A idx;
	final public T tag;

	public Lookup(A idx, T tag) {
		this.idx = idx;
		this.tag = tag;
	}

	@Override public boolean equals(Object o) {
		if (o == this) { return true; }
		if (!(o instanceof Lookup)) { return false; }
		Lookup lku = (Lookup)o;
		return (idx == null && lku.idx == null || idx.equals(lku.idx)) && (tag == null && lku.tag == null || tag.equals(lku.tag));
	}

	@Override public int hashCode() {
		return idx.hashCode() ^ tag.hashCode() ^ 0x6e6e6e6e;
	}

	@Override public String toString() {
		return "(" + idx + ": " + tag + ")";
	}

	public static <T, A> Lookup<T, A> lookup(A idx, T tag) {
		return new Lookup<T, A>(idx, tag);
	}

	public static <T, A> Lookup<T, A> fromTuple(X2<A, T> x) {
		return new Lookup<T, A>(x._0, x._1);
	}

}
