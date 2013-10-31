package jif.extension;

import jif.ast.JifExt;
import jif.types.JifClassType;
import jif.visit.LabelChecker;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>FieldDecl</code> node. 
 * 
 *  @see polyglot.ast.FieldDecl
 */
public interface JifFieldDeclExt extends JifExt {
    void labelCheckField(LabelChecker lc, JifClassType ct)
            throws SemanticException;
}
