// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.MapViewer;

import java.util.Map;

/**
** Utility class that provides generic typesafe viewers.
*/
final public class Viewers {

	private Viewers() { }

	final protected static MapViewer p_view_g = new MapViewer<PTable, Map>() {
		@Override public Map mapFor(PTable tab) {
			return tab.getTGraphs();
		}
	};

	final protected static MapViewer p_view_h = new MapViewer<PTable, Map>() {
		@Override public Map mapFor(PTable tab) {
			return tab.getIndexes();
		}
	};

	@SuppressWarnings("unchecked") public static <A, S> MapViewer<PTable<A, S>, Map<A, S>> PTableTGraphs() {
		return (MapViewer<PTable<A, S>, Map<A, S>>)p_view_g;
	}

	@SuppressWarnings("unchecked") public static <A, S> MapViewer<PTable<A, S>, Map<A, S>> PTableIndexes() {
		return (MapViewer<PTable<A, S>, Map<A, S>>)p_view_h;
	}

}
