// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.MapViewer;

import tags.util.Maps.U2Map;
import tags.util.Union.U2;
import tags.util.Arc;
import java.util.Map;

/**
** Utility class that provides generic typesafe viewers.
**
** TODO NORM maybe find a way to merge this with MultiParts
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

	final protected static MapViewer g_view_n = new MapViewer<TGraph, U2Map>() {
		@Override public U2Map mapFor(TGraph obj) {
			return obj.nodeMap();
		}
	};

	final protected static MapViewer g_view_a = new MapViewer<TGraph, U2Map>() {
		@Override public U2Map mapFor(TGraph obj) {
			return obj.arcMap();
		}
	};

	final protected static MapViewer h_view_a = new MapViewer<Index, U2Map>() {
		@Override public U2Map mapFor(Index obj) {
			return obj.arcMap();
		}
	};

	@SuppressWarnings("unchecked") public static <A, S> MapViewer<PTable<A, S>, Map<A, S>> PTableTGraphs() {
		return (MapViewer<PTable<A, S>, Map<A, S>>)p_view_g;
	}

	@SuppressWarnings("unchecked") public static <A, S> MapViewer<PTable<A, S>, Map<A, S>> PTableIndexes() {
		return (MapViewer<PTable<A, S>, Map<A, S>>)p_view_h;
	}

	@SuppressWarnings("unchecked") public static <T, A, U, W> MapViewer<LocalTGraph<T, A, U, W>, U2Map<T, A, U>> TGraphNodeMap() {
		return (MapViewer<LocalTGraph<T, A, U, W>, U2Map<T, A, U>>)p_view_h;
	}

	@SuppressWarnings("unchecked") public static <T, A, U, W> MapViewer<LocalTGraph<T, A, U, W>, U2Map<Arc<T, T>, Arc<T, A>, W>> TGraphArcMap() {
		return (MapViewer<LocalTGraph<T, A, U, W>, U2Map<Arc<T, T>, Arc<T, A>, W>>)p_view_h;
	}

	@SuppressWarnings("unchecked") public static <T, A, W> MapViewer<LocalIndex<T, A, W>, U2Map<Arc<T, A>, Arc<T, A>, W>> IndexArcMap() {
		return (MapViewer<LocalIndex<T, A, W>, U2Map<Arc<T, A>, Arc<T, A>, W>>)p_view_h;
	}




}
