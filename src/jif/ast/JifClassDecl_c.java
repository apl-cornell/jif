package jif.ast;

import java.util.*;

import jif.types.*;
import polyglot.ast.*;
import polyglot.ext.jl.ast.ClassDecl_c;
import polyglot.ext.param.types.MuPClass;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

/** An implementation of the <code>JifClassDecl</code> interface.
 */
public class JifClassDecl_c extends ClassDecl_c implements JifClassDecl
{
    protected List params;
    protected boolean invariant; //"this" label is invariant
    protected List authority;

    public JifClassDecl_c(Position pos, Flags flags, String name,
	    List params, boolean inv, TypeNode superClass, List interfaces,
	    List authority, ClassBody body) {
	super(pos, flags, name, superClass, interfaces, body);
	this.params = TypedList.copyAndCheck(params, ParamDecl.class, true);
	this.authority = TypedList.copyAndCheck(authority, PrincipalNode.class, true);
	this.invariant = inv;
    }

    public List params() {
	return this.params;
    }

    public JifClassDecl params(List params) {
	JifClassDecl_c n = (JifClassDecl_c) copy();
	n.params = TypedList.copyAndCheck(params, ParamDecl.class, true);
	return n;
    }

    public List authority() {
	return this.authority;
    }

    public JifClassDecl authority(List authority) {
	JifClassDecl_c n = (JifClassDecl_c) copy();
	n.authority = TypedList.copyAndCheck(authority, PrincipalNode.class, true);
	return n;
    }

    protected JifClassDecl_c reconstruct(List params, TypeNode superClass, List interfaces, List authority, ClassBody body) {
	if (! CollectionUtil.equals(params, this.params) || ! CollectionUtil.equals(authority, this.authority)) {
	    JifClassDecl_c n = (JifClassDecl_c) copy();
	    n.params = TypedList.copyAndCheck(params, ParamDecl.class, true);
	    n.authority = TypedList.copyAndCheck(authority, PrincipalNode.class, true);
	    return (JifClassDecl_c) n.reconstruct(superClass, interfaces, body);
	}

	return (JifClassDecl_c) super.reconstruct(superClass, interfaces, body);
    }

    public Node visitChildren(NodeVisitor v) {
	List params = visitList(this.params, v);
	TypeNode superClass = (TypeNode) visitChild(this.superClass, v);
	List interfaces = visitList(this.interfaces, v);
	List authority = visitList(this.authority, v);
	ClassBody body = (ClassBody) visitChild(this.body, v);
	return reconstruct(params, superClass, interfaces, authority, body);
    }

    public Context enterScope(Context c) {
        JifContext A = (JifContext) c;

        JifParsedPolyType ct = (JifParsedPolyType) this.type;
        ClassType inst = ct;

        A = (JifContext) A.pushClass(ct, inst);

        A.setAuthority(new HashSet(ct.authority()));
        for (Iterator i = ct.params().iterator(); i.hasNext(); ) {
            ParamInstance pi = (ParamInstance) i.next();
            A.addVariable(pi);
        }

        return A;
    }

    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        JifClassDecl_c n = (JifClassDecl_c) super.buildTypes(tb);
        n.buildParams((JifTypeSystem) tb.typeSystem());
        return n;
    }

    private void buildParams(JifTypeSystem ts) throws SemanticException {
        JifParsedPolyType ct = (JifParsedPolyType)this.type;
        if (ct == null) {
            // The only way the class type could be null is if
            // super.buildTypes failed. Give up now.
            return;
        }
        MuPClass pc = ts.mutablePClass(ct.position());

        ct.setInstantiatedFrom(pc);
        pc.clazz(ct);

        Set names = new HashSet(params.size());

        List newParams = new ArrayList(params.size());
        for (Iterator i = params.iterator(); i.hasNext();) {
            ParamDecl p = (ParamDecl)i.next();
            newParams.add(p.paramInstance());
            //check for renaming error
            if (names.contains(p.name()))
                throw new SemanticException("Redefined Parameter Error.", p
                        .position());
            else
                names.add(p.name());
        }
        ct.setParams(newParams);

        // set whether the class is invariant.
        ct.setInvariant(this.invariant);
    }


    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        JifClassDecl_c n = (JifClassDecl_c)super.disambiguate(ar);
	
        JifParsedPolyType ct = (JifParsedPolyType) n.type;

        List principals = new ArrayList(n.authority().size());
        for (Iterator i = n.authority().iterator(); i.hasNext(); ) {
            PrincipalNode p = (PrincipalNode) i.next();
            principals.add(p.principal());
        }
        ct.setAuthority(principals);
        return n;
    }

    public JifClassDecl type(Type type) {
        JifClassDecl_c n = (JifClassDecl_c) copy();
        n.type = (ParsedClassType) type;
        return n;
    }

    public void prettyPrintHeader(CodeWriter w, PrettyPrinter tr) {
        if (flags.isInterface()) {
            w.write(flags.clearInterface().clearAbstract().translate());
            w.write("interface ");
        }
        else {
            w.write(flags.translate());
            w.write("class ");
        }

        w.write(name);

        if (! params.isEmpty()) {
            w.write("[");
            for (Iterator i = params.iterator(); i.hasNext(); ) {
                ParamDecl p = (ParamDecl) i.next();
                print(p, w, tr);
                if (i.hasNext()) {
                    w.write(",");
                    w.allowBreak(0, " ");
                }
            }
            w.write("]");
        }

        if (invariant) {
            w.write(" (invariant)");
        }

        if (! authority.isEmpty()) {
            w.write(" authority(");
            for (Iterator i = authority.iterator(); i.hasNext(); ) {
                PrincipalNode p = (PrincipalNode) i.next();
                print(p, w, tr);
                if (i.hasNext()) {
                    w.write(",");
                    w.allowBreak(0, " ");
                }
            }
            w.write(")");
        }

        if (superClass() != null) {
            w.write(" extends ");
            print(superClass(), w, tr);
        }

        if (! interfaces.isEmpty()) {
            if (flags.isInterface()) {
                w.write(" extends ");
            }
            else {
                w.write(" implements ");
            }

            for (Iterator i = interfaces().iterator(); i.hasNext(); ) {
                TypeNode tn = (TypeNode) i.next();
                print(tn, w, tr);

                if (i.hasNext()) {
                    w.write (", ");
                }
            }
        }

        w.write(" {");
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        // The invariantness of the class must agree with its super class
        // and its interfaces.
        Type superClass = null;
        if (this.superClass() != null) {
            superClass = this.superClass().type();
        }
        if (superClass != null && !superClass.equals(tc.typeSystem().Object())) {
            if (this.invariant != ((JifClassType)superClass.toClass()).isInvariant()) {
                if (this.invariant) {
                    throw new SemanticException("Invariant class " + this.type() + " cannot extend covariant class "
                                                + superClass + ". An invariant class may only extend Object or another invariant class",
                                                this.position());                    
                }
                else {
                    throw new SemanticException("Covariant class " + this.type() + " cannot extend invariant class " + superClass,
                                                this.position());                    
                }
            }
        }
        
        JifClassDecl_c n = (JifClassDecl_c)super.typeCheck(tc);
        
        JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
        for (Iterator interfaces = n.type().interfaces().iterator(); interfaces.hasNext(); ) {
            Type interf = (Type)interfaces.next();
            if (ts.unlabel(interf) instanceof JifClassType) {
                if (this.invariant != ((JifClassType)interf).isInvariant()) {
                    if (this.invariant) {
                        throw new SemanticException("Invariant class " + this.type() + " cannot implement covariant interface "
                                                    + interf + ". An invariant class may only implement invariant interfaces",
                                                    this.position());                    
                    }
                    else {
                        throw new SemanticException("Covariant class " + this.type() + " cannot implement invariant interface " + interf,
                                                    this.position());                    
                    }
                }                
            }
        }
              
        return n;
    }

    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }
}
