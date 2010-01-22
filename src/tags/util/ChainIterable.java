// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Arrays;
import java.util.Iterator;

/**
** An {@link Iterable} backed by a chain of {@link Iterable} objects.
**
** @param <T> Type of target
** @author infinity0
*/
public class ChainIterable<T> implements Iterable<T> {

	/**
	** The backing {@link Iterable}.
	*/
	final protected Iterable<? extends Iterable<T>> ibb;

	/**
	** Whether the iterator supports {@link Iterator#remove()}.
	*/
	final public boolean immutable;

	/**
	** DOCUMENT.
	*/
	public ChainIterable(boolean immute, Iterable<T>... i) {
		this(immute, Arrays.asList(i));
	}

	/**
	** DOCUMENT.
	*/
	public ChainIterable(boolean immute, Iterable<? extends Iterable<T>> i) {
		ibb = i;
		immutable = immute;
	}

	/*@Override**/ public Iterator<T> iterator() {
		return immutable? new ChainIterator<T>(ibb) {
			@Override public void remove() { throw new UnsupportedOperationException("Immutable iterator"); }
		}: new ChainIterator<T>(ibb);
	}

	protected static class ChainIterator<T> implements Iterator<T> {

		final Iterator<? extends Iterable<T>> itt;
		Iterator<T> it;
		Iterator<T> ot;

		public ChainIterator(Iterable<? extends Iterable<T>> ibb) {
			this.itt = ibb.iterator();
			this.it = null;
			this.ot = null;
			ensureNext();
		}

		protected void ensureNext() {
			while (it == null || !it.hasNext()) {
				if (!itt.hasNext()) { it = null; break; }
				it = itt.next().iterator();
			}
		}

		@Override public boolean hasNext() {
			return it != null && it.hasNext();
		}

		@Override public T next() {
			if (it == null) { throw new java.util.NoSuchElementException(); }
			T next = it.next();
			ot = it;
			if (!it.hasNext()) { ensureNext(); }
			return next;
		}

		@Override public void remove() {
			if (ot == null) { throw new IllegalStateException(); }
			ot.remove();
		}

	}

}
