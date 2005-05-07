package jif.ast;

import java.util.*;

import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.ext.jl.ast.TypeNode_c;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.*;
import polyglot.visit.*;

/** An implementation of the <code>InstTypeNode</code> interface.
 */
public class InstTypeNode_c extends TypeNode_c implements InstTypeNode, Ambiguous
{
    protected TypeNode base;
    protected List params;
    
    public InstTypeNode_c(Position pos, TypeNode base, List params) {
        super(pos);
        this.base = base;
        this.params = TypedList.copyAndCheck(params, ParamNode.class, true);
    }
    
    public TypeNode base() {
        return this.base;
    }
    
    public InstTypeNode base(TypeNode base) {
        InstTypeNode_c n = (InstTypeNode_c) copy();
        n.base = base;
        return n;
    }
    
    public List params() {
        return this.params;
    }
    
    public InstTypeNode params(List params) {
        InstTypeNode_c n = (InstTypeNode_c) copy();
        n.params = TypedList.copyAndCheck(params, ParamNode.class, true);
        return n;
    }
    
    protected InstTypeNode_c reconstruct(TypeNode base, List params) {
        if (base != this.base || ! CollectionUtil.equals(params, this.params)) {
            InstTypeNode_c n = (InstTypeNode_c) copy();
            n.base = base;
            n.params = TypedList.copyAndCheck(params, ParamNode.class, true);
            return n;
        }
        
        return this;
    }
    
    public Node visitChildren(NodeVisitor v) {
        TypeNode base = (TypeNode) visitChild(this.base, v);
        Type b = base.type();
        
        List newParams = this.params;
        if (b != null && b.isCanonical() && b instanceof JifPolyType) {
            JifPolyType t = (JifPolyType) b;
            newParams = new ArrayList(this.params.size());
            
            Iterator i = t.params().iterator();
            Iterator j = this.params.iterator();
            ParamInstance pi = null;
            while (j.hasNext()) {
                if (i.hasNext()) pi = (ParamInstance)i.next();
                ParamNode p = (ParamNode)j.next();
                if (p instanceof AmbExprParam) {
                    p = ((AmbExprParam)p).expectedPI(pi);
                }
                newParams.add(p);
            }
        }
        
        List params = visitList(newParams, v);
        return reconstruct(base, params);
    }
    
    public boolean isDisambiguated() {
        return false;
    }
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        Type b = base.type();
        
        if (! b.isCanonical()) {
            //  not yet ready to disambiguate
            return this;
        }
        if (! (b instanceof JifPolyType) || ((JifPolyType)b).params().isEmpty()) {
            throw new SemanticException("Cannot instantiate from a non-polymorphic type " + b);
        }
        JifPolyType t = (JifPolyType) b;
        
        List l = new LinkedList();
        
        Iterator i = this.params.iterator();
        Iterator j = t.params().iterator();
        while (i.hasNext() && j.hasNext()) {
            ParamNode p = (ParamNode) i.next();
            ParamInstance pi = (ParamInstance) j.next();
            
            if (pi.isLabel() && !(p.parameter() instanceof Label)) {
                throw new SemanticException("Can not instantiate a "+
                                            "label parameter with a non-label.",
                                            p.position());
            }
            if (pi.isPrincipal() && !(p.parameter() instanceof Principal)) {
                throw new SemanticException("Can not instantiate a "+
                                            "principal parameter with a non-principal.",
                                            p.position());
            }
            if (pi.isInvariantLabel() && !((Label)p.parameter()).isInvariant() )
                throw new SemanticException("Can not instantiate an invariant "+
                                            "label parameter with a non-invariant label.",
                                            p.position());
            
            l.add(p.parameter());
        }
        if (i.hasNext()) {
            throw new SemanticException("Too many parameters supplied for the "
                                        + "class " + t,
                                        this.position());            
        }
        
        return sc.nodeFactory().CanonicalTypeNode(position(),
                                                  ts.instantiate(position(), 
                                                                 t.instantiatedFrom(), l) );
    }
    
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        throw new InternalCompilerError(position(),
                                        "Cannot type check ambiguous node " + this + ".");
    }
    
    public Node exceptionCheck(ExceptionChecker ec) throws SemanticException {
        throw new InternalCompilerError(position(),
                                        "Cannot exception check ambiguous node " + this + ".");
    }
    
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(base, w, tr);
        w.write("[");
        
        for (Iterator i = params.iterator(); i.hasNext(); ) {
            ParamNode p = (ParamNode) i.next();
            print(p, w, tr);
            if (i.hasNext()) {
                w.write(",");
                w.allowBreak(0, " ");
            }
        }
        
        w.write("]");
    }
    
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError(position(),
                                        "Cannot translate ambiguous node " + this + ".");
    }
    
    public String toString() {
        return base + "[...]";
    }
}
