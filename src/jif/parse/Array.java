package jif.parse;

import polyglot.ast.Receiver;
import polyglot.ast.TypeNode;
import polyglot.util.Position;

/**
 * An <code>Array</code> represents a <code>Amb</code> of the form "P[]".  
 * This must be an array type, although the base of that type is still ambiguous.
 */  
public class Array extends Amb {
    // prefix[]
    TypeNode prefix;

    public Array(Grm parser, Position pos, TypeNode prefix) {
	super(parser, pos);
	this.prefix = prefix;
    }

    public TypeNode toType() {
	return parser.nf.ArrayTypeNode(pos, prefix);
    }

    public TypeNode toUnlabeledType() { return toType(); }
    public Receiver toReceiver() { return toType(); }
}

