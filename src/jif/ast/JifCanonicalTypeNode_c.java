package jif.ast;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;
import polyglot.ext.jl.ast.*;
import jif.types.*;


/**
 * A <code>JifCanonicalTypeNode</code> is a type node for a canonical type in Polyj.
 */
public class JifCanonicalTypeNode_c extends CanonicalTypeNode_c implements JifCanonicalTypeNode {
    public JifCanonicalTypeNode_c(Position pos, Type type) {
	super(pos, type);
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
	TypeNode tn = (TypeNode) super.typeCheck(tc);

	JifTypeSystem ts = (JifTypeSystem) tn.type().typeSystem();
	Type t = ts.unlabel(tn.type());

        if (t instanceof JifPolyType) {
            throw new SemanticException("Parameterized type is uninstantiated",
                                        position());
	}

        return tn;
    }
}
