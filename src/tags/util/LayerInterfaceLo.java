// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

/**
** DOCUMENT.
**
** TODO NORM let each request have a <P> param
**
** @param <T> Type of ticket
** @param <L> Type of lower layer
*/
public interface LayerInterfaceLo<T, L extends LayerInterfaceHi> {

	public void receive(T tkt);

	public void setLayerLo(L layerlo);

}
