package jif.lang;

import java.util.*;

public class ReadableByPrinLabel extends AbstractLabel implements Label
{
    private final Principal reader;

    public ReadableByPrinLabel(Principal reader) {
	this.reader = reader;
    }

    public Principal reader() {
	return reader;
    }
    
    public boolean relabelsTo(Label l) {
        if (l instanceof ReadableByPrinLabel) {
            ReadableByPrinLabel rbp = (ReadableByPrinLabel)l;
            return PrincipalUtil.actsFor(rbp.reader, this.reader);
        }
        if (l instanceof JoinLabel) {
            // see if there is a component that we relabel to
            JoinLabel jl = (JoinLabel)l;
            for (Iterator iter = jl.components().iterator(); iter.hasNext(); ) {
                Label comp = (Label)iter.next();
                if (this.relabelsTo(comp)) {
                    return true;
                }
            }
        }
        return false;
    }


    public int hashCode() {
	return (reader==null?0:reader.hashCode());
    }

    public boolean equals(Object o) {
	if (! (o instanceof ReadableByPrinLabel)) {
	    return false;
	}

	ReadableByPrinLabel that = (ReadableByPrinLabel) o;

	return (this.reader == that.reader || 
	        (reader != null && reader.equals(that.reader)
            && that.reader != null && that.reader.equals(reader))); 
    }
    
    public String componentString() {
        return "[readable by " + (reader == null?"<null>":reader.name()) + "]";
    }

}
