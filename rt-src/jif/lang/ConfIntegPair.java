package jif.lang;

import java.util.*;

public class ConfIntegPair implements Label
{
    private final ConfCollection confPols;
    private final IntegCollection integPols;
    
    public ConfIntegPair(ConfCollection confPols, IntegCollection integPols) {
        this.confPols = confPols;
        this.integPols = integPols;
    }
    
    public ConfCollection confPolicies() {
        return confPols;
    }
    public IntegCollection integPolicies() {
        return integPols;
    }
    
    public Label join(Label l) {
        return LabelUtil.label(this).join(l);                
    }
    public Set joinComponents() {
        return Collections.singleton(this);                
    }
    public boolean relabelsTo(Label l) {
        if (l instanceof ConfIntegPair) {
            return relabelsTo((ConfIntegPair)l);
        }
        return LabelUtil.label(this).relabelsTo(l);        
    }
    public boolean relabelsTo(ConfIntegPair l) {
        return this.confPols != null && this.confPols.relabelsTo(l.confPolicies()) &&
        this.integPols != null && this.integPols.relabelsTo(l.integPolicies());
    }
    
    public int hashCode() {
        return (confPols==null?-34:confPols.hashCode()) ^ (integPols==null?25389:integPols.hashCode());
    }
    
    public boolean equals(Object o) {
        if (! (o instanceof ConfIntegPair)) {
            return false;
        }
        
        ConfIntegPair that = (ConfIntegPair) o;
        
        if (this.confPols == that.confPols || 
                (this.confPols != null && this.confPols.equals(that.confPols))) {
            return (this.integPols == that.integPols || 
                    (this.integPols != null && this.integPols.equals(that.integPols)));
        }
        
        return false;
    }
    
    public String componentString() {
        StringBuffer sb = new StringBuffer();
        //TODO needs work
        if (this.confPols != null) {
            sb.append(this.confPols.componentString());
        }
        if (this.integPols != null) {
            sb.append(this.confPols.componentString());
        }
        return sb.toString();
    }
    
}
