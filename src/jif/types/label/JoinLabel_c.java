package jif.types.label;

import java.util.*;

import jif.translate.JoinLabelToJavaExpr_c;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.hierarchy.LabelEnv;
import polyglot.types.*;
import polyglot.util.*;

/** An implementation of the <code>JoinLabel</code> interface. 
 */
public class JoinLabel_c extends Label_c implements JoinLabel
{
    private Set components;
    
    public JoinLabel_c(Collection components, JifTypeSystem ts, Position pos) {
        super(ts, pos, new JoinLabelToJavaExpr_c());
        this.components = Collections.unmodifiableSet(new LinkedHashSet(flatten(components)));
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
    
    public boolean isDisambiguated() {
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            
            if (! c.isDisambiguated()) {
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
            Label c = (Label) i.next();
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
    
    public String componentString(Set printedLabels) {
        String s = "";
        
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            s += c.componentString(printedLabels);
            
            if (i.hasNext()) {
                s += "; ";
            }
        }
        
        return s;
    }
    
    public boolean leq_(Label L, LabelEnv env) {
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
            
            ic.translate(c, w);
            
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
        if (!this.isDisambiguated()) {
            return this;
        }

        Collection comps = flatten(components);
        Set needed = new LinkedHashSet();
        JifTypeSystem jts = (JifTypeSystem) ts;

        for (Iterator i = comps.iterator(); i.hasNext(); ) {
            Label ci = ((Label) i.next()).simplify();
            
            if (ci.hasVariables()) {
                needed.add(ci);
            }
            else {
                boolean subsumed = false;
                
                for (Iterator j = needed.iterator(); j.hasNext(); ) {
                    Label cj = (Label) j.next();
                    
                    if (cj.hasVariables()) {
                        continue;
                    }

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
        
        if (needed.equals(components)) {
            return this;
        }
        if (needed.size() == 1) {
            return (Label)needed.iterator().next();
        }

        return new JoinLabel_c(needed, (JifTypeSystem)ts, position());
    }
    
    private static Collection flatten(Collection comps) {
        Collection c = new LinkedHashSet();
        
        for (Iterator i = comps.iterator(); i.hasNext(); ) {
            Label L = (Label) i.next();
            
            if (L instanceof TopLabel) {
                return Collections.singleton(L);
            }
            
            if (L instanceof JoinLabel_c) {
                Collection lComps = flatten(L.components());
                
                for (Iterator j = lComps.iterator(); j.hasNext(); ) {
                    Label Lj = (Label) j.next();
                    
                    if (Lj instanceof TopLabel) {
                        return Collections.singleton(Lj);
                    }
                    
                    c.add(Lj);
                }
            }
            else {
                c.add(L);
            }
        }
        
        return c;
    }

    public Label subst(LocalInstance arg, Label l) {
        boolean changed = false;
        Set s = new HashSet();
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            Label newc = c.subst(arg, l);
            if (newc != c) changed = true;
            s.add(newc);
        }
        if (!changed) return this;
        return ((JifTypeSystem)typeSystem()).joinLabel(this.position, s);
    }

    /**
     * 
     */
    public Label subst(AccessPathRoot r, AccessPath e) {
        boolean changed = false;
        Set s = new HashSet();
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            Label newc = c.subst(r, e);
            if (newc != c) changed = true;
            s.add(newc);
        }
        if (!changed) return this;
        return ((JifTypeSystem)typeSystem()).joinLabel(this.position, s);
    }

    public Label subst(LabelSubstitution substitution) throws SemanticException {        
        if (components.isEmpty()) {
            return substitution.substLabel(this);
        }
        
        boolean changed = false;
        Set s = new LinkedHashSet();
        
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Label c = (Label) i.next();
            Label newc = c.subst(substitution);
            if (newc != c) changed = true;
            s.add(newc);
        }
        
        if (!changed) return substitution.substLabel(this);
        
        JifTypeSystem ts = (JifTypeSystem)this.typeSystem();
        Label newJoinLabel = ts.joinLabel(this.position(), flatten(s));
        return substitution.substLabel(newJoinLabel).simplify();
    }
}
