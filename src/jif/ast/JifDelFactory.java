package jif.ast;

import polyglot.ast.DelFactory;
import polyglot.ast.JLDel;

public interface JifDelFactory extends DelFactory {

    JLDel delAmbNewArray();

    JLDel delNewLabel();

    JLDel delLabelExpr();

}
