package jif.extension;

import jif.types.JifLocalInstance;
import jif.types.JifTypeSystem;
import jif.types.SemanticDetailedException;
import jif.types.label.ArgLabel;
import jif.types.label.Label;
import jif.visit.ConstChecker;
import polyglot.ast.*;
import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.Goal;
import polyglot.types.ArrayType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.TypeBuilder;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>Formal</code> node. 
 * 
 *  @see polyglot.ast.Formal
 */
public class JifFormalDel extends JifJL_c
{
    public JifFormalDel() { }

    
    private boolean isCatchFormal = false;
    public void setIsCatchFormal(boolean isCatchFormal) {
        this.isCatchFormal = isCatchFormal;
    }
    public boolean isCatchFormal() {
        return this.isCatchFormal;
    }
    
    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        Formal n = (Formal) super.buildTypes(tb);
        JifTypeSystem jts = (JifTypeSystem)tb.typeSystem();

        JifLocalInstance li = (JifLocalInstance)n.localInstance();
        if (isCatchFormal) {
            // formals occuring in a catch clause are treated more like local decls;
            // their label is a VarLabel
            li.setLabel(jts.freshLabelVariable(li.position(), li.name(), "label of the formal " + li.name()));
        }
        else {
            // method and constructor formals have an ArgLabel 
	        ArgLabel al = jts.argLabel(n.position(), li, null);
	        li.setLabel(al);
        }
                
        n = n.localInstance(li);
        return n;
    }

    
    /* Perform an imperative update to the local instance.
     */
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        Formal n = (Formal)super.disambiguate(ar);
        JifTypeSystem jts = (JifTypeSystem)ar.typeSystem();

        JifLocalInstance li = (JifLocalInstance)n.localInstance();
        li.setFlags(n.flags());
        li.setName(n.name());
        li.setType(n.declType());
        //System.err.println("JifFormalDel.disamb: n.type: "  + n.type().getClass() );

        if (!n.type().isDisambiguated()) {
            ar.job().extensionInfo().scheduler().currentGoal().setUnreachableThisRun();
            return n;
	}
        
        
        if (!isCatchFormal) {
            ArgLabel al = (ArgLabel)li.label();
            
            al.setCodeInstance(ar.context().currentCode());
            
            if (al.upperBound() == null) {
                // haven't set the arg label yet
                // do so now.
                
                if (!jts.isLabeled(n.declType())) {
                    // declared type isn't labeled, use the default arg bound
                    Type lblType = n.declType();
                    Position pos = lblType.position();
                    Label defaultBound = jts.defaultSignature().defaultArgBound(n);
                    lblType = jts.labeledType(pos, lblType, defaultBound);
                    n = n.type(n.type().type(lblType));
                }
                
                // now take the label of the declared type, and set it to 
                // be the bound
                al.setUpperBound(jts.labelOfType(n.declType()));
                
                // now set the label of the declared type to be the arg label
                Type lblType = n.declType();
                lblType = jts.labeledType(lblType.position(), jts.unlabel(lblType), al);
                n = n.type(n.type().type(lblType));                
            }
        }

        return n;
    }
    
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        Formal f = (Formal) node();
        
        // if the declared type is an array type, make sure it is the same all the way through
        if (f.localInstance().type().isArray()) {
            JifTypeSystem jts = (JifTypeSystem)tc.typeSystem(); 
            ArrayType at = jts.unlabel(f.localInstance().type()).toArray();
            JifLocalDeclDel.checkArrayTypeConsistency(at);
        }
        return super.typeCheck(tc);
    }
    
}
