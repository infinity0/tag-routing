// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

/**
** DOCUMENT.
**
** @param <T> Type of ticket
** @param <H> Type of upper layer
*/
public interface LayerInterfaceHi<T, H extends LayerInterfaceLo> {

	public T request();

	public void setLayerHi(H layerhi);

}
