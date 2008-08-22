package jif.visit;

import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.TypeSystem;
import polyglot.visit.TypeChecker;

public class JifTypeChecker extends TypeChecker
{
    /**
     * Should type checking attempt to infer missing class parameters?
     */
    private boolean inferClassParameters = false;
    
    public JifTypeChecker(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }
    
    public boolean inferClassParameters() {
        return this.inferClassParameters;
    }
    
    public JifTypeChecker inferClassParameters(boolean inferClassParameters) {
        if (this.inferClassParameters == inferClassParameters) return this;
        JifTypeChecker jtc = (JifTypeChecker)this.copy();
        jtc.inferClassParameters = inferClassParameters;
        return jtc;
    }
       
}
