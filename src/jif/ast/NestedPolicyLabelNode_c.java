package jif.ast;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.label.LabelJ;
import jif.types.label.LabelM;
import jif.types.label.PairLabel;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.*;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>NestedPolicyLabelNode</code> interface.
 */
public class NestedPolicyLabelNode_c extends LabelNode_c implements NestedPolicyLabelNode
{
    protected LabelNode nested;
    
    public NestedPolicyLabelNode_c(Position pos, LabelNode nested) {
        super(pos);
        this.nested = nested;
    }
    
    public LabelNode nested() {
        return this.nested;
    }
    
    protected NestedPolicyLabelNode_c reconstruct(LabelNode nested) {
        if (this.nested != nested) {
            NestedPolicyLabelNode_c n = (NestedPolicyLabelNode_c) copy();
            n.nested = nested;
            return n;
        }
        
        return this;
    }
    
    public Node visitChildren(NodeVisitor v) {
        LabelNode newNested = (LabelNode) visitChild(this.nested, v);
        return reconstruct(newNested);
    }
    
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        if (!nested.isDisambiguated()) {
            sc.job().extensionInfo().scheduler().currentGoal().setUnreachableThisRun();
            return this;
        }
        
        return label(nested.label());
    }
    
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        nested.prettyPrint(w, tr);
    }
}
