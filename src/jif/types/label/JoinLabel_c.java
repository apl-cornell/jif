package jif.types.label;

import java.util.*;

import jif.translate.JoinLabelToJavaExpr_c;
import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.util.*;

/** An implementation of the <code>JoinLabel</code> interface. 
 */
public class JoinLabel_c extends Label_c implements JoinLabel
{
    private Set components;
    
    public JoinLabel_c(Collection components, JifTypeSystem ts, Position pos) {
        super(ts, pos, new JoinLabelToJavaExpr_c());
        this.components = Collections.unmodifiableSet(new LinkedHashSet(components));
        if (isBottom()) {
            setDescription("Bottom of the label lattice, the most public label possible");        
        }
    }
    
    public boolean isBottom() {
        return components.isEmpty();
    }
    public boolean isRuntimeRepresentable() {
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            
            if (! c.isRuntimeRepresentable()) {
                return false;
            }
        }
        
        return true;
    }
    public boolean isCanonical() {
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            
            if (! c.isCanonical()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * @return true iff this label is covariant.
     *
     * A label is covariant if it contains at least one covariant component.
     */
    public boolean isCovariant() {
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            
            if (c.isCovariant()) {
                return true;
            }
        }
        
        return false;
    }
    public boolean isComparable() {
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            
            if (! c.isComparable()) {
                return false;
            }
        }
        
        return true;
    }
    public boolean isEnumerable() {
        return true;
    }
    public Set variables() {
        Set s = new HashSet();
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            LabelImpl c = (LabelImpl) i.next();
            s.addAll(c.variables());
        }
        
        return s;
    }
    
    public boolean equalsImpl(TypeObject o) {
        return this == o; // @@@@@????
    }
    public int hashCode() {
        return components.hashCode();
    }
    
    public String componentString() {
        String s = "";
        
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            LabelImpl c = (LabelImpl) i.next();
            s += c.componentString();
            
            if (i.hasNext()) {
                s += "; ";
            }
        }
        
        return s;
    }
    
    public boolean leq_(Label lbl, LabelEnv env) {
        LabelImpl L = (LabelImpl)lbl;
        if (! L.isSingleton() || ! L.isComparable() || ! L.isEnumerable())
            throw new InternalCompilerError("Cannot compare " + L);
        
        // If this = { .. Pi .. } and L = { .. Pj' .. }, check if for all i,
        // there exists a j, such that Pi <= Pj'
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label ci = (Label) i.next();
            
            if (! env.leq(ci, L)) {
                return false;
            }
        }
        
        return true;
    }
    
    public void translate(Resolver c, CodeWriter w) {
        if (components.isEmpty()) {
            w.write("jif.lang.Label.bottom()");
            return;
        }
        
        boolean first = true;
        
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label ic = (Label) i.next();
            
            ((LabelImpl)ic).translate(c, w);
            
            if (! first) {
                w.write(")");
            }
            
            if (i.hasNext()) {
                w.write(".join(");
            }
            
            first = false;
        }
    }
    
    public Collection components() {
        return Collections.unmodifiableCollection(components);
    }

    /**
     * @return An equivalent label with fewer components by pulling out
     * less restrictive policies.
     */
    public Label simplify() {
        Set needed = new LinkedHashSet();
        JifTypeSystem jts = (JifTypeSystem) ts;
        
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            LabelImpl ci = (LabelImpl)((LabelImpl) i.next()).simplify();
            
            if (ci.hasVariables()) {
                needed.add(ci);
            }
            else {
                boolean subsumed = false;
                
                for (Iterator j = needed.iterator(); j.hasNext(); ) {
                    LabelImpl cj = (LabelImpl) j.next();
                    
                    if (cj.hasVariables()) 
                        continue;
                    
                    if (jts.leq(ci, cj)) {
                        subsumed = true;
                        break;
                    }
                    
                    if (jts.leq(cj, ci)) 
                        j.remove();
                }
                
                if (! subsumed)
                    needed.add(ci);
            }
        }
        
        return flatten(needed, (JifTypeSystem)ts, position());
    }
    private static Label flatten(Collection comps, JifTypeSystem ts, Position pos) {
        Collection c = new LinkedHashSet();
        
        for (Iterator i = comps.iterator(); i.hasNext(); ) {
            Label L = (Label) i.next();
            
            if (L instanceof TopLabel) {
                return L;
            }
            
            if (L instanceof JoinLabel_c) {
                L = flatten(((LabelImpl) L).components(), ts, L.position());
                
                for (Iterator j = ((LabelImpl)L).components().iterator(); j.hasNext(); ) {
                    Label Lj = (Label) j.next();
                    
                    if (Lj instanceof TopLabel) {
                        return Lj;
                    }
                    
                    c.add(Lj);
                }
            }
            else {
                c.add(L);
            }
        }
        
        if (c.size() == 1) {
            return (Label) c.toArray()[0];
        }
        
        return ts.joinLabel(pos, c);
    }
}
