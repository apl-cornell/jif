package jif.visit;

import java.util.*;

import jif.ast.JifConstructorDecl;
import jif.ast.JifMethodDecl;
import jif.ast.LabelNode;
import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.visit.NodeVisitor;

/** 
 * Visits an AST, and applies a <code>LabelSubsitution</code> to all labels
 * that occur in the AST. The <code>LabelSubsitution</code> is not allowed
 * to throw any <code>SemanticException</code>s.
 */
public class LabelSubstitutionVisitor extends NodeVisitor {
    /**
     * Should the Rewriter skip over the 
     */
    private boolean skipBody;
    
    /**
     * The substitution to use.
     */
    private LabelSubstitution substitution;

    /**
     * 
     * @param substitution the LabelSubstitution to use.
     * @param skipBody skip over the body of method/constructor decls?
     */
    public LabelSubstitutionVisitor(LabelSubstitution substitution, 
                                    boolean skipBody) {
        this.skipBody = skipBody;
        this.substitution = substitution;        
    }

    // Don't recurse into the body.
    public Node override(Node n) {
        if (skipBody && n instanceof Block) {
            return n;
        }

        return null;
    }

    public Node leave(Node old, Node n, NodeVisitor v) {
        try {
            if (n instanceof TypeNode) {
                TypeNode c = (TypeNode)n;
                c = rewriteTypeNode(c);
                return c;
            }
            else if (n instanceof Expr) {
                Expr e = (Expr)n;
                e = rewriteExpr(e);

                if (e instanceof Local) {
                    Local lc = (Local)e;
                    LocalInstance li = lc.localInstance();
                    Type t = rewriteType(li.type());

                    // XXX
                    // TODO: fix this. This code currently makes a destructive
                    // update to the local instance; this is due to an assumption
                    // in other places of the code that there is only a single
                    // local instance shared by all appropriate Locals. We will 
                    // maintain that assumption here, although the long-term fix
                    // is to fix the other code that assumes this.
                    li.setType(t);
                    return lc;
                    //                return lc.localInstance(li.type(t));
                }
                return e;
            }
            else if (n instanceof LabelNode) {
                LabelNode ln = (LabelNode)n;
                Label l = rewriteLabel(ln.label());
                ln = ln.label(l);
                return ln;
            }
            else if (n instanceof Formal) {
                Formal fn = (Formal)n;

                JifLocalInstance li = (JifLocalInstance)fn.localInstance();
                Type t = rewriteType(li.type());

                // XXX
                // TODO: fix this. This code currently makes a destructive
                // update to the local instance; this is due to an assumption
                // in other places of the code that there is only a single
                // local instance shared by all appropriate Formals. We will 
                // maintain that assumption here, although the long-term fix
                // is to fix the other code that assumes this.
                li.setType(t);
                li.setLabel(rewriteLabel(li.label()));
                return fn;
                //            return fn.localInstance(li.type(t));
            }
            else if (n instanceof LocalDecl) {
                LocalDecl ld = (LocalDecl)n;

                JifLocalInstance li = (JifLocalInstance)ld.localInstance();
                Type t = rewriteType(li.type());

                // XXX
                // TODO: fix this. This code currently makes a destructive
                // update to the local instance; this is due to an assumption
                // in other places of the code that there is only a single
                // local instance shared by all appropriate Locals. We will 
                // maintain that assumption here, although the long-term fix
                // is to fix the other code that assumes this.
                li.setType(t);
                li.setLabel(rewriteLabel(li.label()));
                return ld;
                //            return ld.localInstance(li.type(t));
            }
            else if (n instanceof FieldDecl) {
                FieldDecl fd = (FieldDecl)n;

                JifFieldInstance fi = (JifFieldInstance)fd.fieldInstance();
                Type t = rewriteType(fi.type());

                // XXX
                // TODO: fix this. This code currently makes a destructive
                // update to the local instance; this is due to an assumption
                // in other places of the code that there is only a single
                // local instance shared by all appropriate Locals. We will 
                // maintain that assumption here, although the long-term fix
                // is to fix the other code that assumes this.
                fi.setType(t);
                fi.setLabel(rewriteLabel(fi.label()));
                
                return fd;
                //            return ld.localInstance(li.type(t));
            }
            else if (n instanceof ProcedureDecl) {
                ProcedureDecl md = (ProcedureDecl)n;

                JifProcedureInstance mi = (JifProcedureInstance)md.procedureInstance();
                mi.setReturnLabel(rewriteLabel(mi.returnLabel()), mi.isDefaultReturnLabel());
                mi.setStartLabel(rewriteLabel(mi.startLabel()), mi.isDefaultStartLabel());
                
                List throwTypes = new ArrayList(mi.throwTypes());
                for (int i = 0; i < throwTypes.size(); i++) {
                    throwTypes.set(i, rewriteType((Type)throwTypes.get(i)));
                }
                List formalTypes = new ArrayList(mi.formalTypes());
                for (int i = 0; i < formalTypes.size(); i++) {
                    formalTypes.set(i, rewriteType((Type)formalTypes.get(i)));
                }

                if (mi instanceof JifMethodInstance) {
                    JifMethodInstance jmi = (JifMethodInstance)mi;
                    jmi.setReturnType(rewriteType(jmi.returnType()));
                    
                    jmi.setThrowTypes(throwTypes);
                    jmi.setFormalTypes(formalTypes);
                    md = ((JifMethodDecl)md).methodInstance(jmi);
                }
                else if (mi instanceof JifConstructorInstance) {
                    JifConstructorInstance jci = (JifConstructorInstance)mi;
                    
                    jci.setThrowTypes(throwTypes);
                    jci.setFormalTypes(formalTypes);
                    md = ((JifConstructorDecl)md).constructorInstance(jci);
                }

                return md;
            }

            return n;
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException "+
                "thrown: " + e.getMessage(), n.position());
        }
    }

    /**
     * Replace the args in the label of type nodes.
     */
    public TypeNode rewriteTypeNode(TypeNode tn) throws SemanticException {
        Type t = tn.type();
        return tn.type(rewriteType(t));
    }

    public Expr rewriteExpr(Expr e) throws SemanticException {
        Type t = e.type();
        return e.type(rewriteType(t));        
    }

    public Type rewriteType(Type t) throws SemanticException {
        if (t instanceof LabeledType) {
            LabeledType lt = (LabeledType)t;
            Label L = lt.labelPart();
            Type bt = lt.typePart();
            return lt.labelPart(rewriteLabel(L)).typePart(rewriteType(bt));
        }
        else if (t instanceof ArrayType) {
            ArrayType at = (ArrayType)t;
            return at.base(rewriteType(at.base()));
        }
        else if (t instanceof JifSubstType) {
            JifSubstType jst = (JifSubstType)t;
            Map newMap = new HashMap();
            List args = new LinkedList();
            boolean diff = false;

            for (Iterator i = jst.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Object arg = e.getValue();
                Param p;
                if (arg instanceof Label) {
                    p = (Label)rewriteLabel((Label)arg);
                }
                else if (arg instanceof Principal) {
                    p = (Principal)rewritePrincipal((Principal)arg);
                }
                else {
                    throw new InternalCompilerError(
                        "Unexpected type for entry: "
                            + arg.getClass().getName());
                }
                newMap.put(e.getKey(), p);

                if (p != arg) {
                    diff = true;
                }
            }
            if (diff) {
                JifTypeSystem ts = (JifTypeSystem)t.typeSystem();
                t = ts.subst(jst.base(), newMap);
                return t;
            }

        }
        return t;
    }

    protected Label rewriteLabel(Label L) throws SemanticException {
        if (L == null) return L;
        return L.subst(substitution);
    }
    protected Principal rewritePrincipal(Principal p) throws SemanticException {
        if (p == null) return p;
        return p.subst(substitution);
    }
}
