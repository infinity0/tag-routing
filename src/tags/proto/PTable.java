// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import java.util.Map;

/**
** Preference table. DOCUMENT.
**
** @param <A> Type of address
** @param <S> Type of score
*/
public class PTable<A, S> {

	//final protected A addr;

	final protected Map<A, S> tgraphs;
	final protected Map<A, S> indexes;

	public PTable(/*A addr, */Map<A, S> tgraphs, Map<A, S> indexes) {
		//this.addr = addr;
		this.tgraphs = tgraphs;
		this.indexes = indexes;
	}

	public Map<A, S> getTGraphs() {
		return tgraphs;
	}

	public Map<A, S> getIndexes() {
		return indexes;
	}

}
