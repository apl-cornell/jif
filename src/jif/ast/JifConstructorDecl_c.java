package jif.ast;

import java.util.*;

import jif.types.*;
import jif.types.label.Label;
import polyglot.ast.*;
import polyglot.ext.jl.ast.ConstructorDecl_c;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

/** 
 * An implementation of the <code>JifConstructor</code> interface.
 */
public class JifConstructorDecl_c extends ConstructorDecl_c implements JifConstructorDecl
{
    protected LabelNode startLabel;
    protected LabelNode returnLabel;
    protected List constraints;

    public JifConstructorDecl_c(Position pos, Flags flags, String name, LabelNode startLabel, LabelNode returnLabel, List formals, List throwTypes, List constraints, Block body) {
	super(pos, flags, name, formals, throwTypes, body);
	this.startLabel = startLabel;
        this.returnLabel = returnLabel;
	this.constraints = TypedList.copyAndCheck(constraints, ConstraintNode.class, true);
    }

    public LabelNode startLabel() {
	return this.startLabel;
    }

    public JifConstructorDecl startLabel(LabelNode startLabel) {
	JifConstructorDecl_c n = (JifConstructorDecl_c) copy();
	n.startLabel = startLabel;
	return n;
    }

    public LabelNode returnLabel() {
        return this.returnLabel;
    }

    public JifConstructorDecl returnLabel(LabelNode returnLabel) {
	JifConstructorDecl_c n = (JifConstructorDecl_c) copy();
	n.returnLabel = returnLabel;
	return n;
    }

    public List constraints() {
	return this.constraints;
    }

    public JifConstructorDecl constraints(List constraints) {
	JifConstructorDecl_c n = (JifConstructorDecl_c) copy();
	n.constraints = TypedList.copyAndCheck(constraints, ConstraintNode.class, true);
	return n;
    }

    protected JifConstructorDecl_c reconstruct(LabelNode startLabel, LabelNode returnLabel, List formals, List throwTypes, List constraints, Block body) {
	if (startLabel != this.startLabel || returnLabel != this.returnLabel || !CollectionUtil.equals(constraints, this.constraints)) {
	    JifConstructorDecl_c n = (JifConstructorDecl_c) copy();
	    n.startLabel = startLabel;
	    n.returnLabel = returnLabel;
	    n.constraints = TypedList.copyAndCheck(constraints, ConstraintNode.class, true);
	    return (JifConstructorDecl_c) n.reconstruct(formals, throwTypes, body);
	}

	return (JifConstructorDecl_c) super.reconstruct(formals, throwTypes, body);
    }

    public Node visitChildren(NodeVisitor v) {
        LabelNode startLabel = (LabelNode) visitChild(this.startLabel, v);
        LabelNode returnLabel = (LabelNode) visitChild(this.returnLabel, v);
        List formals = visitList(this.formals, v);
        List throwTypes = visitList(this.throwTypes, v);
	List constraints = visitList(this.constraints, v);
	Block body = (Block) visitChild(this.body, v);
	return reconstruct(startLabel, returnLabel, formals, throwTypes, constraints, body);
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        JifConstructorDecl n = (JifConstructorDecl_c)super.disambiguate(ar);

        JifConstructorInstance jci = (JifConstructorInstance)n.constructorInstance();
        JifTypeSystem jts = (JifTypeSystem)ar.typeSystem();

        if (n.startLabel() != null && !n.startLabel().isDisambiguated()) {
            // the startlabel node hasn't been disambiguated yet
            return n;
        }

        if (n.returnLabel() != null && !n.returnLabel().isDisambiguated()) {
            // the return label node hasn't been disambiguated yet
            return n;
        }

        // set the formal types
        List formalTypes = new ArrayList(n.formals().size());
        for (Iterator i = n.formals().iterator(); i.hasNext(); ) {
            Formal f = (Formal)i.next();
            if (!f.isDisambiguated()) {
                // formals are not disambiguated yet.
                return n;
            }
            formalTypes.add(f.declType());
        }
        jci.setFormalTypes(formalTypes);

        Label Li; // start label
        boolean isDefaultStartLabel = false;
        DefaultSignature ds = jts.defaultSignature();
        if (n.startLabel() == null) {
            Li = ds.defaultStartLabel(n.position(), n.name());
            isDefaultStartLabel = true;
        } 
        else {
            Li = n.startLabel().label();
        }
        jci.setStartLabel(Li, isDefaultStartLabel);
    
        Label Lr; // return label
        boolean isDefaultReturnLabel = false;
        if (n.returnLabel() == null) {
            Lr = ds.defaultReturnLabel(n);
            isDefaultReturnLabel = true;
        }
        else {
            Lr = n.returnLabel().label();
        }        
        jci.setReturnLabel(Lr, isDefaultReturnLabel);

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
        jci.setThrowTypes(throwTypes);
        
        List constraints = new ArrayList(n.constraints().size());
        for (Iterator i = n.constraints().iterator(); i.hasNext(); ) {
            ConstraintNode cn = (ConstraintNode) i.next();
            if (!cn.isDisambiguated()) {
                // constraint nodes haven't been disambiguated yet.
                return n;
            }
            constraints.add(cn.constraint());
        }
        jci.setConstraints(constraints);

        return n.constructorInstance(jci);
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        
	Node n = super.typeCheck(tc);    
        JifConstructorDecl_c jcd = (JifConstructorDecl_c)n;
        jcd.checkConstructorCall(tc);
    
        return jcd;
    }
    
    /**
     * Checks that if there is an explicit constructor call in the constructor
     * body that the call is alright.
     * 
     * In particular, if this is a java class or one of the ancestors of this 
     * class is "untrusted" then the explicit constructor call must be 
     * the first statement in the constructor body.
     * 
     * Moreover, if this is a Jif class, but the superclass is not a Jif class,
     * then first statement must be a default constructor call.
     * @throws SemanticException
     */
    private void checkConstructorCall(TypeChecker tc) throws SemanticException {
        
        JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();

        ClassType ct = tc.context().currentClass();

        // ignore java.lang.Object
        if (ts.equals(ct, ts.Object())) 
            return;

        ClassType untrusted = ts.hasUntrustedAncestor(ct);
        if (!ts.isJifClass(ct) || untrusted != null) {
            // the first statement of the body had better be an explicit
            // constructor call
            StringBuffer message = new StringBuffer("The first statement " + 
                               "of the constructor must be a constructor " +
                               "call");               
            if (untrusted != null) {
                message.append(", as the ancestor ");
                message.append(untrusted.fullName());
                message.append(" is not trusted");
            }
            message.append(".");
            checkFirstStmtConstructorCall(message.toString(), false);
        }
        
        if (ts.isJifClass(ct) && !ts.isJifClass(ct.superType())) {
            // this is a Jif class, but it's super class is not.
            // the first statement must be a default constructor call
            checkFirstStmtConstructorCall("The first statement of the " +
                           "constructor must be a default constructor call, " +
                           "as the superclass is not a Jif class", 
                           true);
        }
    }
    
    private void checkFirstStmtConstructorCall(String message, 
                                               boolean mustBeDefaultCall) 
                                 throws SemanticException {
        if (body().statements().size() < 1) {
            throw new SemanticException("Empty constructor body.",
                                        position());
        }
        Stmt s = (Stmt)body().statements().get(0);
        if (!(s instanceof ConstructorCall)) {
            throw new SemanticException(message, position());
        }
        else if (mustBeDefaultCall) {
            ConstructorCall cc = (ConstructorCall)s;
            if (cc.arguments().size() > 0) {
                throw new SemanticException(message, position());                
            }
        }
        
    }
}
