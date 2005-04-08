package jif.ast;

import jif.types.JifTypeSystem;
import jif.types.JifVarInstance;
import jif.types.label.Label;
import polyglot.ast.Node;
import polyglot.types.*;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <tt>AmbDynamicLabel</tt> interface. */
public class AmbDynamicLabelNode_c extends AmbLabelNode_c implements AmbDynamicLabelNode
{
    protected String name;

    public AmbDynamicLabelNode_c(Position pos, String name) {
	super(pos);
	this.name = name;
    }

    public String toString() {
	return "*" + name;
    }

    /** Gets the name. */
    public String name() {
	return this.name;
    }

    /** Returns a copy of this node with the name updated. */
    public AmbDynamicLabelNode name(String name) {
	AmbDynamicLabelNode_c n = (AmbDynamicLabelNode_c) copy();
	n.name = name;
	return n;
    }

    /** Disambiguate the type of this node. */
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
	Context c = sc.context();
	JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
	JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

        VarInstance vi = c.findVariable(name);
//        if (!vi.isCanonical()) {
//            // the instance is not yet ready
//            return this;
//        }
        
//        if (vi == null || ind >= 0) {
//            JifProcedureInstance pi = (JifProcedureInstance)c.currentCode();
//            Formal formal = jar.formalForArg(ind);
//            
//            if (formal != null && (vi == null || vi == formal.localInstance())) {
//                JifLocalInstance li = (JifLocalInstance) formal.localInstance();
//                Label L = ts.dynamicArgLabel(position(), li.uid(), 
//                    name(), li.label(), ind, true);
//                return nf.CanonicalLabelNode(position(), L);
//            }
//            
//            if (vi == null) {
//                // the following will fail with an appropriate exception
//                c.findVariable(name);
//            }
//        }

	if (vi instanceof JifVarInstance && vi.flags().isFinal()) {
	    // we will check that it is a label later.

	    JifVarInstance jvi = (JifVarInstance) vi;

	    Label L = ts.dynamicLabel(position(), JifUtil.varInstanceToAccessPath(jvi));
	    return nf.CanonicalLabelNode(position(), L);
	}

	throw new SemanticException(vi + " is not a final variable " +
	    "of type \"label\".");
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("*");
        w.write(name);
    }
}
