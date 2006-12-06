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
    final TypeNode prefix;
    final boolean isConst;

    public Array(Grm parser, Position pos, TypeNode prefix) {
        this(parser, pos, prefix, false);
    }
    public Array(Grm parser, Position pos, TypeNode prefix, boolean isConst) {
        super(parser, pos);
        this.prefix = prefix;
        this.isConst = isConst;
    }

    public TypeNode toType() {
        if (isConst) {
            return parser.nf.ConstArrayTypeNode(pos, prefix);
        }
        return parser.nf.ArrayTypeNode(pos, prefix);
    }

    public TypeNode toUnlabeledType() { return toType(); }
    public Receiver toReceiver() { return toType(); }
}

