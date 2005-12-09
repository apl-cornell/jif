package jif.types;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.util.Position;

/** A Jif label with names for the debuging use. 
 */
public class NamedLabel
{
    protected Map nameToLabels;
    protected Map nameToDescrip;

    protected Label label;
    protected Position pos;
    
    public NamedLabel() {
        this(null);
    }
    
    public NamedLabel(String name, Label l) {
        this(name, null, l);
    }
    
    public NamedLabel(String name, String descrip, Label l) {
        this(l.position());
        nameToLabels.put(name, l);
        if (descrip != null) {
            nameToDescrip.put(name, descrip);
        }
        
        label = l;
        if (pos == null) pos = l.position();
    }

    public NamedLabel(Position pos) {
        this.nameToLabels = new LinkedHashMap();
        this.nameToDescrip = new LinkedHashMap();
	this.label = null;
	this.pos = pos;
    }
    
    public Position position() {
	return pos;
    }
    
    public NamedLabel join(LabelChecker lc, String name, Label l) {
        return join(lc, name, null, l);
        
    }
    public NamedLabel join(LabelChecker lc, String name, String descrip, Label l) {
        nameToLabels.put(name, l);
        if (descrip != null) {
            nameToDescrip.put(name, descrip);
        }
        
	if (label==null) {
	    label = l;
	    if (pos == null) pos = l.position();
	}
        else {
            label = lc.upperBound(label, l);
        }
	return this;
    }
    
    public Label label() {
	return label;
    }
    
    public String toString() {
	StringBuffer sb = new StringBuffer();
	for (Iterator iter = nameToLabels.keySet().iterator(); iter.hasNext(); ) {
	    String name = (String) iter.next();
	    sb.append(name);
	    if (iter.hasNext()) 
                sb.append(" + ");
	}

	return sb.toString();
    }
    
    public Label label(String name) {
	return (Label) nameToLabels.get(name);
    }
}
