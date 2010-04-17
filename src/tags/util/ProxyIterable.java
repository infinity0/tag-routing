// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Iterator;

/**
** A proxy to a backing {@link Iterable}. The {@link #nextFor(Object)} method
** must be overridden to convert source items to target items.
**
** @param <S> Type of source
** @param <T> Type of target
** @author infinity0
*/
abstract public class ProxyIterable<S, T> implements Iterable<T> {

	public enum Mutability {
		/** {@link Iterator#remove()} throws {@link UnsupportedOperationException} */
		IMMUTABLE,
		/** {@link Iterator#remove()} falls through to the backing iterator */
		MUTABLE,
		/** {@link Iterator#remove()} falls through to the backing iterator, then calls {@link #removeFor(Object)}. */
		REMOVE_CLEANUP
	}

	/**
	** The backing {@link Iterable}.
	*/
	final protected Iterable<S> ib;

	/**
	** @see Mutability
	*/
	final public Mutability mute;

	/**
	** Create a new mutable iterable backed by the given iterable.
	*/
	public ProxyIterable(Iterable<S> i) {
		this(i, Mutability.MUTABLE);
	}

	/**
	** Create a new iterable backed by the given iterable, with the given
	** {@link Mutability} setting. Note that mutability will only have an
	** effect if the backing iterator is also mutable.
	*/
	public ProxyIterable(Iterable<S> ib, Mutability mute) {
		this.ib = ib;
		this.mute = mute;
	}

	/*@Override**/ public Iterator<T> iterator() {
		switch (mute) {
		case IMMUTABLE:
			return new Iterator<T>() {
				final Iterator<S> it = ib.iterator();
				/*@Override**/ public boolean hasNext() { return it.hasNext(); }
				/*@Override**/ public T next() { return ProxyIterable.this.nextFor(it.next()); }
				/*@Override**/ public void remove() { throw new UnsupportedOperationException("immutable iterator"); }
			};
		case MUTABLE:
			return new Iterator<T>() {
				final Iterator<S> it = ib.iterator();
				/*@Override**/ public boolean hasNext() { return it.hasNext(); }
				/*@Override**/ public T next() { return ProxyIterable.this.nextFor(it.next()); }
				/*@Override**/ public void remove() { it.remove(); }
			};
		case REMOVE_CLEANUP:
			return new Iterator<T>() {
				final Iterator<S> it = ib.iterator();
				S last = null;
				/*@Override**/ public boolean hasNext() { return it.hasNext(); }
				/*@Override**/ public T next() { last = it.next(); return ProxyIterable.this.nextFor(last); }
				/*@Override**/ public void remove() { it.remove(); assert last != null; ProxyIterable.this.removeFor(last); }
			};
		}
		throw new AssertionError();
	}

	/**
	** Returns an object of the target type given an object of the source type.
	*/
	abstract protected T nextFor(S elem);

	/**
	** Performs extra cleanup operations after the given element is removed.
	*/
	protected void removeFor(S elem) { }

}
