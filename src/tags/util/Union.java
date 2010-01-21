// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Arrays;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;

/**
** Implementation of union types.
*/
final public class Union {

	private Union() { }

	public static class U0 {

		final protected Object val;
		final protected int type;

		/**
		** @throws NullPointerException if {@code val} is {@code null}
		*/
		protected U0(Object val, int type) {
			if (val == null) {
				throw new NullPointerException();
			}
			this.val = val;
			this.type = type;
		}

		protected void check(int type) {
			if (this.type != type) { throw new IllegalStateException("Not the correct type"); }
		}

		@Override public boolean equals(Object o) {
			if (o == this) { return true; }
			if (!(o instanceof U0)) { return false; }
			U0 u = (U0)o;
			return val.equals(u.val) && type == u.type;
		}

		@Override public int hashCode() {
			return val.hashCode() + 1 + type;
		}

	}

	public static class U1<T0> extends U0 {
		protected U1(Object val, int type) { super(val, type); }
		@SuppressWarnings("unchecked") final public T0 getT0() { check(0); return (T0)val; }
	}

	public static class U2<T0, T1> extends U1<T0> {
		protected U2(Object val, int type) { super(val, type); }
		@SuppressWarnings("unchecked") final public T1 getT1() { check(1); return (T1)val; }
	}

	public static class U3<T0, T1, T2> extends U2<T0, T1> {
		protected U3(Object val, int type) { super(val, type); }
		@SuppressWarnings("unchecked") final public T2 getT2() { check(2); return (T2)val; }
	}


	public static <T0> U1<T0> U1_0(T0 val) { return new U1<T0>(val, 0); }

	public static <T0, T1> U2<T0, T1> U2_0(T0 val) { return new U2<T0, T1>(val, 0); }
	public static <T0, T1> U2<T0, T1> U2_1(T1 val) { return new U2<T0, T1>(val, 1); }

	public static <T0, T1, T2> U3<T0, T1, T2> U3_0(T0 val) { return new U3<T0, T1, T2>(val, 0); }
	public static <T0, T1, T2> U3<T0, T1, T2> U3_1(T1 val) { return new U3<T0, T1, T2>(val, 1); }
	public static <T0, T1, T2> U3<T0, T1, T2> U3_2(T2 val) { return new U3<T0, T1, T2>(val, 2); }

}
