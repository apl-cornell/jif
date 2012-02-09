package jif.visit;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Expr;
import polyglot.ast.IntLit;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * For convenience in writing signatures, we allow native constructors (with
 * empty bodies). Since this is not allowed in Java, this pass adds them back in
 * before translation.
 * 
 * @author mdgeorge
 */
public class NativeConstructorAdder extends NodeVisitor {
 
    private final NodeFactory nf;
    
    public NativeConstructorAdder(final NodeFactory nf) {
        this.nf = nf;
    }
    
    @Override
    public Node override(Node n) {
        if (n instanceof ConstructorDecl) {
            ConstructorDecl decl = (ConstructorDecl) n;
            ClassType       ct   = decl.constructorInstance().container().toClass();
            
            if (decl.body() == null) {
                ConstructorCall dummy = dummyCall(ct);
                Block superCall = nf.Block(Position.COMPILER_GENERATED, dummy); 
                return decl.body(superCall);
            }
            else
                return decl;
        }
        else
            return null;
    }
    
    /**
     * Construct a dummy super call.
     */
    private ConstructorCall dummyCall(ClassType ct) {
        ClassType sup = ct.superType().toClass();
        List<ConstructorInstance> cxs = sup.constructors();
        ConstructorInstance ci = cxs.isEmpty() ? ct.typeSystem().defaultConstructor(Position.COMPILER_GENERATED, sup)
                                               : cxs.get(0);
        
        List<Expr> args = new ArrayList<Expr>();
        for (Type t : (List<Type>) ci.formalTypes())
            args.add(dummyValue(t));
        
        return nf.ConstructorCall(ct.position(), ConstructorCall.SUPER, args).constructorInstance(ci);
    }
    
    /**
     * Create a dummy expr with having the given type 
     */
    private Expr dummyValue(Type t) {
        Expr e = null;
        if (t.isNumeric())
            e = nf.IntLit(Position.COMPILER_GENERATED, IntLit.INT, 0);
        else if (t.isBoolean())
            e = nf.BooleanLit(Position.COMPILER_GENERATED, false);
        else
            e = nf.NullLit(Position.COMPILER_GENERATED);
        
        return e.type(t);
    }

}
