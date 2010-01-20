// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

/**
** DOCUMENT.
**
** @param <T> Type of tag
*/
public class AddressScheme<T> {

	final protected List<T> node_list = new ArrayList<T>();
	final protected Map<T, Integer> node = new HashMap<T, Integer>();

	final protected Map<T, Set<T>> outgoing = new HashMap<T, Set<T>>();
	final protected Map<T, Set<T>> incoming = new HashMap<T, Set<T>>();

	/**
	** Construct a new scheme with the given tag as the zeroth (ie. seed) tag.
	*/
	public AddressScheme(T src) {
		pushNode(src, null);
	}

	public T getSeedTag() {
		return node_list.get(0);
	}

	// TODO HIGH code some getter methods for these

	/**
	** Attaches the given tag to the address scheme, with a set of incoming
	** neighbours. Only tags already in the scheme (ie. nearer to the seed tag)
	** will be added as incoming neighbours; the rest will be filtered out.
	**
	** @param tag The node to push onto this address scheme
	** @param inc The incoming neighbours of the node
	** @throws IllegalArgumentException if the scheme already contains {@code
	**         tag}, or if {@code inc} contains it
	*/
	protected void pushNode(T tag, Set<T> inc) {
		if (node.containsKey(tag)) {
			throw new IllegalArgumentException("scheme already contains tag: " + tag);
		}
		if (inc.contains(tag)) {
			throw new IllegalArgumentException("cannot accept an incoming set that defines a loop");
		}

		int i = node_list.size();
		Set<T> tinc = new HashSet<T>();

		node_list.add(tag);
		node.put(tag, i);
		outgoing.put(tag, new HashSet<T>());
		incoming.put(tag, tinc);

		if (inc == null || inc.isEmpty()) { return; }
		for (T t: inc) {
			if (node.containsKey(t)) {
				outgoing.get(t).add(tag);
				tinc.add(t);
			}
		}
	}

}
