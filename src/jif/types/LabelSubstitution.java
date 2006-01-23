package jif.types;

import java.util.LinkedList;

import jif.types.label.Label;
import jif.types.label.Policy;
import jif.types.principal.Principal;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;

/**
 * The class is a simple Label visitor. It can be used to substitute and check
 * labels and principals. All labels and principals implement a method
 * <code>subst(LabelSubstitution)</code>. 
 * 
 * <p>In addition, the LabelSubstitution keeps a stack of labels, to record which
 * labels it is in the middle of processing. Labels that recurse on nested
 * labels must push themselves onto the stack before the recursive call,
 * and pop themselves off the stack after the recursive call. Prior to pushing
 * themselves on the stack, labels should examine the stack to see if they 
 * already exist in the stack; if so, then the label should not make a recursive
 * call, in order to avoid an infinite loop. 
 * 
 * @see jif.types.label.Label#subst(LabelSubstitution)
 */
public abstract class LabelSubstitution {
    public Label substLabel(Label L) throws SemanticException {
        return L;
    }
    public Policy substPolicy(Policy p) throws SemanticException {
        return p;
    }
    public Principal substPrincipal(Principal p) throws SemanticException {
        return p;
    }    
    
    private LinkedList stack = null;
    public void pushLabel(Label l) {
        pushLabel((Object)l);
    }
    private void pushLabel(Object l) {
        if (stack == null) {
            stack = new LinkedList();
        }
        stack.addLast(l);
    }
    public void popLabel(Label l) {
        if (l != stack.removeLast()) {
            throw new InternalCompilerError("Stack discipline not obeyed");
        }
    }
    public boolean stackContains(Label l) {
        return stack != null && stack.contains(l);
    }
}
