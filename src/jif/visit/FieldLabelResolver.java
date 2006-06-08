package jif.visit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jif.extension.JifFieldDeclExt;
import jif.types.*;
import jif.types.label.Label;
import jif.types.label.VarLabel;
import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

/** A visitor used to resolving field labels. We want to resolving
 *  field labels of all the classes before the label checking pass,
 *  because these field labels might be included in other labels, thus
 *  need to resolve first.
 */
public class FieldLabelResolver extends ContextVisitor
{
    private final Job job;
    private final JifTypeSystem ts;
    private VarMap bounds;
    private Map fieldVarBounds;
    
    public FieldLabelResolver(Job job, JifTypeSystem ts, NodeFactory nf) {
	super(job, ts, nf);
	this.job = job;
	this.ts = ts;
    }

    public NodeVisitor enterCall(Node n) throws SemanticException {
        if (n instanceof ClassMember && ! (n instanceof ClassDecl)) {
            return bypassChildren(n);
        }

        if (n instanceof ClassDecl) {
            this.fieldVarBounds = new HashMap();
        }
        
	if (n instanceof ClassBody) {
	    // labelcheck the class body
	    ClassBody d = (ClassBody) n;
	    labelCheckClassBody(d);
	}

        return this;
    }

    private void labelCheckClassBody(ClassBody d) throws SemanticException {
        JifClassType ct = (JifClassType) context().currentClassScope();

        LabelChecker lc = new LabelChecker(job, ts, nodeFactory(), false, false, false);

        if (lc == null) {
            throw new InternalCompilerError("Could not label check " +
                                            ct + ".", d.position());
        }

        for (Iterator i = d.members().iterator(); i.hasNext(); ) {
            ClassMember m = (ClassMember) i.next();

            if (m instanceof FieldDecl) {
                JifFieldDeclExt ext = (JifFieldDeclExt) m.ext();
                ext.labelCheckField(lc, ct);
            }
        }

        Solver solver = lc.solver();
        this.bounds = solver.solve();        
    }

    public Node leaveCall(Node old, Node n, NodeVisitor v) throws SemanticException {
        if (n instanceof FieldDecl) {
            FieldDecl f = (FieldDecl) n;
            JifFieldInstance fi = (JifFieldInstance) f.fieldInstance();
            if (fi.label() instanceof VarLabel) {
                this.fieldVarBounds.put(fi.label(), 
                                        bounds.boundOf((VarLabel)fi.label()));
            }

            Type lbledType = ts.labeledType(f.declType().position(), 
                                            ts.unlabel(f.declType()), 
                                            fi.label());
            return f.type(f.type().type(lbledType));
        
        }

        if (n instanceof ClassBody) {
            // need to go through the entire class body and replace the 
            // variables that we have now solved for.
            LabelSubstitutionVisitor lsv = 
                new LabelSubstitutionVisitor(new FieldVarLabelSubst(this.fieldVarBounds), 
                                             false);
            n =  n.visitChildren(lsv);

        }
        return n;
    }
    
    private static class FieldVarLabelSubst extends LabelSubstitution {
        Map map;
        public FieldVarLabelSubst(Map fieldVarBounds) {
            map = fieldVarBounds;
        }
        public Label substLabel(Label L) throws SemanticException {
            Label b = (Label)map.get(L);            
            if (b != null) {
                return b;
            }
            return L;
        }        
    }
}
