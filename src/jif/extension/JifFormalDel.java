package jif.extension;

import jif.types.JifLocalInstance;
import jif.types.JifTypeSystem;
import jif.types.label.ArgLabel;
import jif.types.label.Label;
import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.TypeBuilder;

/** The Jif extension of the <code>Formal</code> node. 
 * 
 *  @see polyglot.ast.Formal
 */
public class JifFormalDel extends JifJL_c
{
    public JifFormalDel() { }

    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        Formal n = (Formal) super.buildTypes(tb);
        JifTypeSystem jts = (JifTypeSystem)tb.typeSystem();

        JifLocalInstance li = (JifLocalInstance)n.localInstance();
        ArgLabel al = jts.argLabel(n.position(), li);
        li.setLabel(al);
                
        return n.localInstance(li);
    }

    
    /* Perform an imperative update to the local instance.
     */
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        Formal n = (Formal)super.disambiguate(ar);
        JifTypeSystem jts = (JifTypeSystem)ar.typeSystem();
        
        
        if (!n.declType().isCanonical()) {
            return n;
        }
        
        // if there is no declared label of the type, use the default arg bound
        if (!jts.isLabeled(n.declType())) {
            Type lblType = n.declType();
            Position pos = lblType.position();
            Label defaultBound = jts.defaultSignature().defaultArgBound(n);
            
            lblType = jts.labeledType(pos, lblType, defaultBound);
            n = n.type(n.type().type(lblType));
        }

        JifLocalInstance li = (JifLocalInstance)n.localInstance();
        if (n.declType().isCanonical()) {
            li.setType(n.declType());
        }   

        
        // set the bound of the arg label to the declared label of the formal type
        ArgLabel al = (ArgLabel)li.label();
        al.setUpperBound(jts.labelOfType(n.declType()));
                       
        return n;
    }
}
