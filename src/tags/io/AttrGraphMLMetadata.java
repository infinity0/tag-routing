// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.io;

import edu.uci.ics.jung.io.GraphMLMetadata;

import org.apache.commons.collections15.Transformer;

/**
** Reimplementation of {@link GraphMLMetadata} which has extra methods for
** casting attribute-values to their correct attribute-types, and also uses
** final fields.
**
** @see <a href="http://www.graphviz.org/doc/info/attrs.html">Node, Edge and Graph Attributes</a>
*/
public class AttrGraphMLMetadata<T> {

	public static enum AttrType { BOOLEAN, INT, LONG, FLOAT, DOUBLE, STRING }

	final public AttrType type;
	final public String default_value;
	final public String description;

	final protected Transformer<T, ?> transformer;
	protected Transformer<T, String> _transformer;

	public AttrGraphMLMetadata(AttrType type, String default_value, String description, Transformer<T, ?> transformer) {
		this.type = type;
		this.default_value = default_value;
		this.description = description;
		this.transformer = transformer;
	}

	final public boolean isBoolean() { return type == AttrType.BOOLEAN; }
	final public boolean isInteger() { return type == AttrType.INT; }
	final public boolean isLong() { return type == AttrType.LONG; }
	final public boolean isFloat() { return type == AttrType.FLOAT; }
	final public boolean isDouble() { return type == AttrType.DOUBLE; }
	final public boolean isString() { return type == AttrType.STRING; }

	public Transformer<T, String> transformer() {
		if (_transformer == null) {
			_transformer = new Transformer<T, String>() {

				@Override public String transform(T key) {
					Object o = transformer.transform(key);
					return o == null? null: o.toString();
				}

			};
		}
		return _transformer;
	}

	@SuppressWarnings("unchecked")
	public Transformer<T, Boolean> transformerBoolean() {
		if (isBoolean()) { return (Transformer<T, Boolean>)transformer; }
		else { throw new IllegalStateException("incorrect type"); }
	}

	@SuppressWarnings("unchecked")
	public Transformer<T, Integer> transformerInteger() {
		if (isInteger()) { return (Transformer<T, Integer>)transformer; }
		else { throw new IllegalStateException("incorrect type"); }
	}

	@SuppressWarnings("unchecked")
	public Transformer<T, Long> transformerLong() {
		if (isLong()) { return (Transformer<T, Long>)transformer; }
		else { throw new IllegalStateException("incorrect type"); }
	}

	@SuppressWarnings("unchecked")
	public Transformer<T, Float> transformerFloat() {
		if (isFloat()) { return (Transformer<T, Float>)transformer; }
		else { throw new IllegalStateException("incorrect type"); }
	}

	@SuppressWarnings("unchecked")
	public Transformer<T, Double> transformerDouble() {
		if (isDouble()) { return (Transformer<T, Double>)transformer; }
		else { throw new IllegalStateException("incorrect type"); }
	}

	@SuppressWarnings("unchecked")
	public Transformer<T, String> transformerString() {
		if (isString()) { return (Transformer<T, String>)transformer; }
		else { throw new IllegalStateException("incorrect type"); }
	}

}
