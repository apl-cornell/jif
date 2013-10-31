package jif.extension;

import jif.ast.JifExt_c;
import jif.translate.ToJavaExt;
import jif.visit.LabelChecker;
import polyglot.ast.ClassDecl;
import polyglot.ast.Ext;
import polyglot.ast.LocalClassDecl;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class JifLocalClassDeclExt extends JifExt_c implements Ext {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifLocalClassDeclExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        LocalClassDecl lcd = (LocalClassDecl) node();
        // LabelChecker isn't a proper vistor. 
        // Copy it to avoid destructive side effects.
//        lc = (LabelChecker) lc.copy();
        return lcd.decl((ClassDecl) lc.labelCheck(lcd.decl()));
    }
}
