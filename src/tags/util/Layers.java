// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

/**
** DOCUMENT.
*/
final public class Layers {

	private Layers() { }

	public static <T, H extends LayerInterfaceLo<T, L>, L extends LayerInterfaceHi<T, H>> void combine(H hi, L lo) {
		hi.setLayerLo(lo);
		lo.setLayerHi(hi);
	}

}
