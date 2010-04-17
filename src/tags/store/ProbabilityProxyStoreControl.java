// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.store;

import tags.util.Maps;

import tags.proto.PTable;
import tags.util.Union.U2;
import tags.util.Maps.U2Map;
import tags.util.ProxyMap;
import tags.util.Probability;
import java.util.Map;

import java.io.IOException;

/**
** A proxy to a backing {@link StoreControl} whose attribute types (U, W, S, Z)
** are all {@link Double}, providing a view of these as {@link Probability}
** instead.
**
** @param <I> Type of identity
** @param <T> Type of tag
** @param <A> Type of address
*/
public class ProbabilityProxyStoreControl<I, T, A> implements StoreControl<I, T, A, Probability, Probability, Probability, Probability> {

	final protected StoreControl<I, T, A, Double, Double, Double, Double> sctl;

	public ProbabilityProxyStoreControl(StoreControl<I, T, A, Double, Double, Double, Double> sctl) {
		this.sctl = sctl;
	}

	@Override public Map<I, Probability> getFriends(I id) throws IOException {
		return probabilityProxyMap(sctl.getFriends(id));
	}

	@Override public PTable<A, Probability> getPTable(I id) throws IOException {
		PTable<A, Double> ptb = sctl.getPTable(id);
		return new PTable<A, Probability>(probabilityProxyMap(ptb.getTGraphs()), probabilityProxyMap(ptb.getIndexes()));
	}

	@Override public U2Map<T, A, Probability> getTGraphOutgoing(A addr, T src) throws IOException {
		U2Map<T, A, Double> out = sctl.getTGraphOutgoing(addr, src);
		return Maps.uniteDisjoint(probabilityProxyMap(out.K0Map()), probabilityProxyMap(out.K1Map()));
	}

	@Override public Probability getTGraphNodeAttr(A addr, U2<T, A> node) throws IOException {
		return new Probability(sctl.getTGraphNodeAttr(addr, node));
	}

	@Override public U2Map<A, A, Probability> getIndexOutgoing(A addr, T src) throws IOException {
		U2Map<A, A, Double> out = sctl.getIndexOutgoing(addr, src);
		return Maps.uniteDisjoint(probabilityProxyMap(out.K0Map()), probabilityProxyMap(out.K1Map()));
	}

	public static <K> Map<K, Probability> probabilityProxyMap(final Map<K, Double> map) {
		return new ProxyMap<K, Double, Probability>(map) {
			@Override public Probability itemFor(Double d) {
				return new Probability(d);
			}
		};
	}

}
