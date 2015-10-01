package jif.ast;

import jif.extension.JifFormalDel;
import jif.types.JifTypeSystem;
import polyglot.ast.Ext;
import polyglot.ast.Formal_c;
import polyglot.ast.Id;
import polyglot.ast.TypeNode;
import polyglot.types.Flags;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/**
 * 
 */
// XXX Should be replaced with extension
@Deprecated
public class JifFormal_c extends Formal_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

//    @Deprecated
    public JifFormal_c(Position pos, Flags flags, TypeNode type, Id name) {
        this(pos, flags, type, name, null);
    }

    public JifFormal_c(Position pos, Flags flags, TypeNode type, Id name,
            Ext ext) {
        super(pos, flags, type, name, ext);
    }

    /**
     * 
     */
    @Override
    public boolean isDisambiguated() {
        boolean typeNotNull = type() != null && type().type() != null;
        JifTypeSystem jts = (JifTypeSystem) (typeNotNull
                ? type().type().typeSystem() : null);
        return super.isDisambiguated()
                && (((JifFormalDel) del()).isCatchFormal()
                        || (type() != null && type().type() != null
                                && jts.isLabeled(type().type())));
    }
}
