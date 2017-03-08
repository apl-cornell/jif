package jif.ast;

import jif.types.Assertion;
import polyglot.ast.Ext;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;

/** An implementation of the <code>CanonicalConstraint</code>. 
 */
public class CanonicalConstraintNode_c extends ConstraintNode_c<Assertion>
        implements CanonicalConstraintNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

//    @Deprecated
    public CanonicalConstraintNode_c(Position pos, Assertion constraint) {
        this(pos, constraint, null);
    }

    public CanonicalConstraintNode_c(Position pos, Assertion constraint,
            Ext ext) {
        super(pos, ext);
        this.setConstraint(constraint);
    }

    @Override
    public Assertion constraint() {
        return super.constraint();
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(constraint().toString());
    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("Cannot translate " + this);
    }
}
