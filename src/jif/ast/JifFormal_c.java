package jif.ast;

import jif.types.JifTypeSystem;
import polyglot.ast.TypeNode;
import polyglot.ext.jl.ast.Formal_c;
import polyglot.types.Flags;
import polyglot.util.Position;

/**
 * 
 */
public class JifFormal_c extends Formal_c {

    public JifFormal_c(Position pos, Flags flags, TypeNode type, String name) {
        super(pos, flags, type, name);
    }

    WHERE DO FORMAL TYPES GET LABELED?
//    /**
//     * 
//     */
//    public boolean isDisambiguated() {
//        boolean typeNotNull = type() != null && type().type() != null;
//        JifTypeSystem jts = (JifTypeSystem)(typeNotNull ? type().type().typeSystem() : null);
//        return super.isDisambiguated() && 
//                type() != null && 
//                type().type() != null &&
//                jts.isLabeled(type().type());
//    }
}
