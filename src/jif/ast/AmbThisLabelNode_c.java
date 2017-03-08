package jif.ast;

import jif.types.JifClassType;
import jif.types.JifContext;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>AmbThisLabelNode</code> interface. 
 */
public class AmbThisLabelNode_c extends AmbLabelNode_c
        implements AmbThisLabelNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

//    @Deprecated
    public AmbThisLabelNode_c(Position pos) {
        this(pos, null);
    }

    public AmbThisLabelNode_c(Position pos, Ext ext) {
        super(pos, ext);
    }

    @Override
    public String toString() {
        return "this{amb}";
    }

    /** Disambiguates the type of this node by finding the correct label for
     * "this". 
     */
    @Override
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        JifContext c = (JifContext) sc.context();
        JifClassType ct = (JifClassType) c.currentClass();

        if (ct == null || (c.inStaticContext() && !c.inConstructorCall())) {
            throw new SemanticException("The label \"this\" cannot be used "
                    + "in a static context.", position());
        }

        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

        if (!ct.thisLabel(null).isCanonical()) {
            sc.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }

        return nf.CanonicalLabelNode(position(), ct.thisLabel(null));
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("this");
    }
}
