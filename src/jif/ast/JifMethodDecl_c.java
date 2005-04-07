package jif.ast;

import java.util.*;

import jif.types.*;
import jif.types.JifMethodInstance;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.*;
import polyglot.ext.jl.ast.MethodDecl_c;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;

/** An implementation of the <code>JifMethod</code> interface.
 */
public class JifMethodDecl_c extends MethodDecl_c implements JifMethodDecl
{
    protected LabelNode startLabel;
    protected LabelNode returnLabel;
    protected List constraints;

    public JifMethodDecl_c(Position pos, Flags flags, TypeNode returnType,
	    String name, LabelNode startLabel, List formals, LabelNode returnLabel,
	    List throwTypes, List constraints, Block body) {
	super(pos, flags, returnType, name, formals, throwTypes, body);
	this.startLabel = startLabel;
	this.returnLabel = returnLabel;
	this.constraints = TypedList.copyAndCheck(constraints, 
		ConstraintNode.class, true);    
    }

    public LabelNode startLabel() {
	return this.startLabel;
    }

    public JifMethodDecl startLabel(LabelNode startLabel) {
	JifMethodDecl_c n = (JifMethodDecl_c) copy();
	n.startLabel = startLabel;
	return n;
    }

    public LabelNode returnLabel() {
	return this.returnLabel;
    }

    public JifMethodDecl returnLabel(LabelNode returnLabel) {
	JifMethodDecl_c n = (JifMethodDecl_c) copy();
	n.returnLabel = returnLabel;
	return n;
    }

    public List constraints() {
	return this.constraints;
    }

    public JifMethodDecl constraints(List constraints) {
	JifMethodDecl_c n = (JifMethodDecl_c) copy();
	n.constraints = TypedList.copyAndCheck(constraints, ConstraintNode.class, true);
	return n;
    }

    public Node visitChildren(NodeVisitor v) {
        TypeNode returnType = (TypeNode) visitChild(this.returnType, v);
	LabelNode startLabel = (LabelNode) visitChild(this.startLabel, v);
        List formals = visitList(this.formals, v);
	LabelNode returnLabel = (LabelNode) visitChild(this.returnLabel, v);
        List throwTypes = visitList(this.throwTypes, v);
	List constraints = visitList(this.constraints, v);
	Block body = (Block) visitChild(this.body, v);
	return reconstruct(returnType, startLabel, formals, returnLabel, throwTypes, constraints, body);
    }

    protected JifMethodDecl_c reconstruct(TypeNode returnType, LabelNode startLabel, List formals, LabelNode returnLabel, List throwTypes, List constraints, Block body) {
      if (startLabel != this.startLabel || returnLabel != this.returnLabel || ! CollectionUtil.equals(constraints, this.constraints)) {
          JifMethodDecl_c n = (JifMethodDecl_c) copy();
          n.startLabel = startLabel;
          n.returnLabel = returnLabel;
          n.constraints = TypedList.copyAndCheck(constraints, ConstraintNode.class, true);
          return (JifMethodDecl_c) n.reconstruct(returnType, formals, throwTypes, body);
      }

      return (JifMethodDecl_c) super.reconstruct(returnType, formals, throwTypes, body);
    }
    
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        JifMethodDecl n = (JifMethodDecl)super.disambiguate(ar);

        JifMethodInstance jmi = (JifMethodInstance)n.methodInstance();
        JifTypeSystem jts = (JifTypeSystem)ar.typeSystem();

        // return type
        if (!n.returnType().isDisambiguated()) {
            // return type node not disambiguated yet
            return n;
        }

        DefaultSignature ds = jts.defaultSignature();
        
        Type declrt = n.returnType().type();
        if (! declrt.isVoid() && !jts.isLabeled(declrt)) {
            // return type isn't labeled. Add the default label.
            declrt = jts.labeledType(declrt.position(), declrt, ds.defaultReturnValueLabel(n));
            n = (JifMethodDecl)n.returnType(n.returnType().type(declrt));
            jmi.setReturnType(declrt);
        }

        if (n.startLabel() != null && !n.startLabel().isDisambiguated()) {
            // the startlabel node hasn't been disambiguated yet
            return n;
        }

        if (n.returnLabel() != null && !n.returnLabel().isDisambiguated()) {
            // the return label node hasn't been disambiguated yet
            return n;
        }

        Label Li; // start label
        boolean isDefaultStartLabel = false;
        if (n.startLabel() == null) {
            Li = ds.defaultStartLabel(n.position(), n.name());
            isDefaultStartLabel = true;
        } 
        else {
            Li = n.startLabel().label();
        }
        jmi.setStartLabel(Li, isDefaultStartLabel);

        Label Lr; // return label
        boolean isDefaultReturnLabel = false;
        if (n.returnLabel() == null) {
            Lr = ds.defaultReturnLabel(n);
            isDefaultReturnLabel = true;
        }
        else {
            Lr = n.returnLabel().label();
        }        
        jmi.setReturnLabel(Lr, isDefaultReturnLabel);
        
        // set the formal arg labels and formal types
        List formalArgLabels = new ArrayList(n.formals().size());
        List formalTypes = new ArrayList(n.formals().size());
        for (Iterator i = n.formals().iterator(); i.hasNext(); ) {
            Formal f = (Formal)i.next();
            if (!f.isDisambiguated()) {
                // formals are not disambiguated yet.
                return n;
            }
            JifLocalInstance jli = (JifLocalInstance)f.localInstance();
            formalArgLabels.add(jli.label());
            formalTypes.add(f.declType());
        }
        jmi.setFormalArgLabels(formalArgLabels);
        jmi.setFormalTypes(formalTypes);
        

        // set the labels for the throwTypes.
        List throwTypes = new LinkedList();        
        for (Iterator i = n.throwTypes().iterator(); i.hasNext();) {
            TypeNode tn = (TypeNode)i.next();
            if (!tn.isDisambiguated()) {
                // throw types haven't been disambiguated yet.
                return n;
            }
            
            Type xt = tn.type();
            if (!jts.isLabeled(xt)) {
                // default exception label is the return label
                xt = jts.labeledType(xt.position(), xt, Lr);
            }
            throwTypes.add(xt);
        }
        jmi.setThrowTypes(throwTypes);

        List constraints = new ArrayList(n.constraints().size());
        for (Iterator i = n.constraints().iterator(); i.hasNext(); ) {
            ConstraintNode cn = (ConstraintNode) i.next();
            if (!cn.isDisambiguated()) {
                // constraint nodes haven't been disambiguated yet.
                return n;
            }
            constraints.add(cn.constraint());
        }
        jmi.setConstraints(constraints);
        return n.methodInstance(jmi);
    }
}

/**
 *  This class substitutes all signature ArgLabels, DynamicArgLabels and
 * ArgPrincipals in a procedure declaration with appropriate non-signature
 * labels/principals.
 */
//class SignatureArgSubstitution extends ArgLabelSubstitution {
//    public SignatureArgSubstitution(List nonSigArgLabels) {
//        super(nonSigArgLabels, false);
//    }
//    public Label substLabel(Label L) {
//        L = super.substLabel(L);
//
//        if (L instanceof DynamicArgLabel) {
//            DynamicArgLabel dal = (DynamicArgLabel)L;
//            JifTypeSystem jts = (JifTypeSystem)dal.typeSystem();
//            L = jts.dynamicArgLabel(dal.position(), 
//                                    dal.uid(), 
//                                    dal.name(), 
//                                    dal.label(), 
//                                    dal.index(), 
//                                    false);
//        }
//        return L;
//    }
//
//    public Principal substPrincipal(Principal p) {
//        p = super.substPrincipal(p);
//        if (p instanceof ArgPrincipal) {
//            ArgPrincipal dap = (ArgPrincipal)p;
//            JifTypeSystem jts = (JifTypeSystem)dap.typeSystem();
//            p = jts.argPrincipal(dap.position(),
//                                 dap.uid(),
//                                 dap.name(),
//                                 dap.label(),
//                                 dap.index(),
//                                 false);
//        }
//        return p;
//    }
//}
