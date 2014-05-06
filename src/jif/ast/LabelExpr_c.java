package jif.ast;

import java.util.List;

import jif.types.JifTypeSystem;
import polyglot.ast.Expr_c;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;
import polyglot.visit.TypeChecker;

/** An implementation of the <code>NewLabel</code> interface.
 */
public class LabelExpr_c extends Expr_c implements LabelExpr {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected LabelNode label;

//    @Deprecated
    public LabelExpr_c(Position pos, LabelNode label) {
        this(pos, label, null);
    }

    public LabelExpr_c(Position pos, LabelNode label, Ext ext) {
        super(pos, ext);
        this.label = label;
    }

    @Override
    public LabelNode label() {
        return this.label;
    }

    @Override
    public LabelExpr label(LabelNode label) {
        LabelExpr_c n = (LabelExpr_c) copy();
        n.label = label;
        return n;
    }

    protected LabelExpr_c reconstruct(LabelNode label) {
        if (label != this.label) {
            LabelExpr_c n = (LabelExpr_c) copy();
            n.label = label;
            return n;
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        LabelNode label = visitChild(this.label, v);
        return reconstruct(label);
    }

    /**
     * @throws SemanticException
     */
    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();
        return type(ts.Label());
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        return label().label().throwTypes(ts);
    }

    @Override
    public Term firstChild() {
        return null;
    }

    @Override
    public <T> List<T> acceptCFG(CFGBuilder<?> v, List<T> succs) {
        return succs;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("{");
        print(label, w, tr);
        w.write("}");
    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }

    @Override
    public String toString() {
        return label.toString();
    }
}
