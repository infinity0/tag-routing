// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import tags.util.Tuple.X2;

/**
** Immutable 2-tuple representing an arc.
**
** @param <S> Type of source node
** @param <T> Type of target node
*/
public class Arc<S, T> {

	final public S src;
	final public T dst;

	public Arc(S src, T dst) {
		this.src = src;
		this.dst = dst;
	}

	@Override public boolean equals(Object o) {
		if (o == this) { return true; }
		if (!(o instanceof Arc)) { return false; }
		Arc arc = (Arc)o;
		return (src == null && arc.src == null || src.equals(arc.src)) && (dst == null && arc.dst == null || dst.equals(arc.dst));
	}

	@Override public int hashCode() {
		return src.hashCode() ^ dst.hashCode();
	}

	@Override public String toString() {
		return "<" + src + ", " + dst + ">";
	}

	public static <S, T> Arc<S, T> arc(S src, T dst) {
		return new Arc<S, T>(src, dst);
	}

	public static <S, T> Arc<S, T> fromTuple(X2<S, T> x) {
		return new Arc<S, T>(x._0, x._1);
	}

}
