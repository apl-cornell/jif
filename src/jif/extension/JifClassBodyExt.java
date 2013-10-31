package jif.extension;

import java.util.LinkedList;
import java.util.List;

import jif.ast.JifExt_c;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.visit.LabelChecker;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassMember;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

/** The extension of the <code>ClassBody</code> node.
 * 
 *  @see polyglot.ast.ClassBody
 */
public class JifClassBodyExt extends JifExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifClassBodyExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) {
        ClassBody n = (ClassBody) node();

        JifTypeSystem jts = lc.typeSystem();

        JifContext A = lc.context();
        A = (JifContext) n.del().enterScope(A);
        A.setCurrentCodePCBound(jts.notTaken());
        lc = lc.context(A);

        //find all the final fields that have an initializer
        List<ClassMember> members = new LinkedList<ClassMember>();

        for (ClassMember cm : n.members()) {
            try {
                // labelCheck() imperatively updates LabelChecker.solver.
                // The solver should only be used for checking this particular
                // member.
                LabelChecker lc_ = (LabelChecker) lc.copy();
                members.add((ClassMember) lc_.context(A).labelCheck(cm));
            } catch (SemanticException e) {
                // report it and keep going.
                lc.reportSemanticException(e);
            }
        }

        return n.members(members);
    }
}
