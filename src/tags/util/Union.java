// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Arrays;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;

/**
** Implementation of union types. These will be type-safe if you use them
** properly. :p
**
** TODO LOW it might make more sense to do U1 extends U2 extends U3 and have
** the extra methods throw {@link UnsupportedOperationException} or smth... no
** time to think this through right now...
*/
final public class Union {

	private Union() { }

	/**
	** Base class for unions. This is non-instantiable by definition ("0
	** possible choices in the types of its members").
	*/
	public static class U0 {

		final public Object val;
		final public int type;

		/**
		** @throws NullPointerException if {@code val} is {@code null}
		*/
		private U0(Object val, int type) {
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

	/**
	** An immutable 1-union.
	*/
	public static class U1<T0> extends U0 {
		private U1(Object val, int type) { super(val, type); }
		final public boolean isT0() { return type == 0; }
		@SuppressWarnings("unchecked") final public T0 getT0() { check(0); return (T0)val; }
	}

	/**
	** An immutable 2-union.
	*/
	public static class U2<T0, T1> extends U1<T0> {
		private U2(Object val, int type) { super(val, type); }
		final public boolean isT1() { return type == 1; }
		@SuppressWarnings("unchecked") final public T1 getT1() { check(1); return (T1)val; }
	}

	/**
	** An immutable 3-union.
	*/
	public static class U3<T0, T1, T2> extends U2<T0, T1> {
		private U3(Object val, int type) { super(val, type); }
		final public boolean isT2() { return type == 2; }
		@SuppressWarnings("unchecked") final public T2 getT2() { check(2); return (T2)val; }
	}

	/**
	** Creates a new {@link U1} with a value of type {@code T0}.
	*/
	public static <T0> U1<T0> U1_0(T0 val) { return new U1<T0>(val, 0); }

	/**
	** Creates a new {@link U2} with a value of type {@code T0}.
	*/
	public static <T0, T1> U2<T0, T1> U2_0(T0 val) { return new U2<T0, T1>(val, 0); }

	/**
	** Creates a new {@link U2} with a value of type {@code T1}.
	*/
	public static <T0, T1> U2<T0, T1> U2_1(T1 val) { return new U2<T0, T1>(val, 1); }

	/**
	** Creates a new {@link U3} with a value of type {@code T0}.
	*/
	public static <T0, T1, T2> U3<T0, T1, T2> U3_0(T0 val) { return new U3<T0, T1, T2>(val, 0); }

	/**
	** Creates a new {@link U3} with a value of type {@code T1}.
	*/
	public static <T0, T1, T2> U3<T0, T1, T2> U3_1(T1 val) { return new U3<T0, T1, T2>(val, 1); }

	/**
	** Creates a new {@link U3} with a value of type {@code T2}.
	*/
	public static <T0, T1, T2> U3<T0, T1, T2> U3_2(T2 val) { return new U3<T0, T1, T2>(val, 2); }

}
