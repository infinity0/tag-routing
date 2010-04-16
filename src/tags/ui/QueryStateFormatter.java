// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.ui;

import tags.proto.AddressScheme;

import java.util.Map;
import java.util.Set;

/**
** An interface for formatting various state objects of an ongoing query, such
** as the address scheme and completed lookups.
*/
public interface QueryStateFormatter<T, A, W> {

	public String formatResults(Map<A, W> results);

	public String[] formatLookups(Map<A, Set<T>> lookups, Set<T> tags);

	public String[] formatAddressScheme(AddressScheme<T, A, W> scheme);

}
