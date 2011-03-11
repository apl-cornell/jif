package jif.ast;

import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.label.ConfPolicy;
import jif.types.label.IntegPolicy;
import jif.types.label.Label;
import jif.types.label.PairLabel;
import polyglot.ast.Expr_c;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.StringUtil;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;
import polyglot.visit.TypeChecker;

/** An implementation of the <code>LabelNode</code> interface. 
 */
public abstract class LabelNode_c extends Expr_c implements LabelNode
{
    private Label label;

    public LabelNode_c(Position pos) {
        super(pos);
    }

    protected LabelNode_c(Position pos, Label label) {
        super(pos);
        this.label = label;
    }

    @Override
    public boolean isDisambiguated() {
        return label != null && label.isCanonical() && super.isDisambiguated();
    }

    @Override
    public Label label() {
	return label;
    }

    @Override
    public LabelNode label(Label label) {
	LabelNode_c n = (LabelNode_c) copy();
	n.label = label;
	return n;
    }

    @Override
    public Label parameter() {
	return label();
    }
    
    @Override
    public LabelNode parameter(Label label) {
        return label(label);
    }

    @Override
    public String toString() {
	if (label != null) {
	    return label.toString();
	}
	else {
	    return "<unknown-label>";
	}
    }

    /**
     * @throws SemanticException  
     */
    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) tc.typeSystem();
        return type(ts.Label());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List throwTypes(TypeSystem ts) {
        return label().throwTypes(ts);
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(this.toString());
    }
    
    @Override
    public void dump(CodeWriter w) {
        if (label != null) {
            w.write("(" + StringUtil.getShortNameComponent(label.getClass().getName()) + " ");
            if (label instanceof PairLabel) {
                PairLabel pl = (PairLabel) label;
                ConfPolicy cp = pl.confPolicy();
                IntegPolicy ip = pl.integPolicy();
                w.write("{" + StringUtil.getShortNameComponent(cp.getClass().getName()) + " " + cp.toString() + ";" +
                        StringUtil.getShortNameComponent(ip.getClass().getName()) + " " + ip.toString() + "}" + ")");
            } else {
                w.write(label.toString());
            }
        } else {
            w.write("<null-label>");
        }
    }    
    
    @Override
    public final void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }
    
}
