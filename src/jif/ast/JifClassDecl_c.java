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

        //"this" label attached to ClassType. Essentially, it is a
        //covariant label parameter.
        JifParsedPolyType ct = (JifParsedPolyType) n.type;
        JifTypeSystem jts = (JifTypeSystem)tb.typeSystem();
        ct.setThisLabel(jts.unknownLabel(ct.position()));
        
        return n;
    }

    private void buildParams(JifTypeSystem ts) throws SemanticException {
        JifParsedPolyType ct = (JifParsedPolyType)this.type;
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

        if (invariant) {
            ct.invariant(true);

            /*
             * NJN -- not necessary
             * 
             * //create a new param instance ParamLabel pl = (ParamLabel)
             * ct.thisLabel(); ct.addParam(ts.paramInstance(this.position(), ct,
             * ParamInstance.INVARIANT_LABEL, pl.uid() ) );
             *  
             */
            /*
             * Type st = ct.superType(); if (st instanceof JifClassType) {
             * JifClassType jst = (JifClassType) st; if (jst.invariant()) { Type
             * newSt = jst.setInvariantThis(ct.thisLabel()); if (st != newSt)
             * ct.superType(newSt); } }
             */
        }
    }


    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        JifClassDecl_c n = (JifClassDecl_c)super.disambiguate(ar);
	
        JifParsedPolyType ct = (JifParsedPolyType) n.type;
        //@@@@@Need to disamb the params? Probably....

        List principals = new ArrayList(n.authority().size());
        for (Iterator i = n.authority().iterator(); i.hasNext(); ) {
            PrincipalNode p = (PrincipalNode) i.next();
            principals.add(p.principal());
        }
        ct.setAuthority(principals);

        if (this.superClass() == null || this.superClass().isDisambiguated()) {
            // Super type is disambiguated now--can add params.
            if (invariant) {
                Type st = ct.superType();

                if (st instanceof JifClassType) {
                    JifClassType jst = (JifClassType) st;
                    if (jst.invariant()) {
                        Type newSt = jst.setInvariantThis(ct.thisLabel());
                        if (st != newSt) 
                            ct.superType(newSt);
                    }
                }
            }	
        }

        // set the this label
        JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
        if (!ct.thisLabel().isCanonical()) {
            if (!invariant)
                ct.setThisLabel(ts.covariantThisLabel(position(), ct));
            else
                ct.setThisLabel(ts.thisLabel(position(), ct));
        }
        
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
        // The invariantness of the class must agree with its super class.
        // At the moment, this means that we can't have any invariant classes.
        Type superClass = null;
        if (this.superClass() != null) {
            superClass = this.superClass().type();
        }
        if (superClass != null) {
            if (this.invariant != ((JifClassType)superClass.toClass()).invariant()) {
                throw new SemanticException("Covariant classes other than " +
                     "Object can only be " +
                    "extended with covariant classes.", 
                    this.position());                
            }
        }
                
        return super.typeCheck(tc);
    }

    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }
}
