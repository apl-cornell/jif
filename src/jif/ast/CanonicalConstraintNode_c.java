package jif.ast;

import jif.types.Assertion;
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

    public CanonicalConstraintNode_c(Position pos, Assertion constraint) {
        super(pos);
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
