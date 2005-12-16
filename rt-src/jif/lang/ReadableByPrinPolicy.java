package jif.lang;

import java.util.*;

public class ReadableByPrinPolicy implements ConfPolicy
{
    private final Principal reader;

    public ReadableByPrinPolicy(Principal reader) {
	this.reader = reader;
    }

    public Principal reader() {
	return reader;
    }
    
    public boolean relabelsTo(Policy l) {
        if (l instanceof ReadableByPrinPolicy) {
            ReadableByPrinPolicy rbp = (ReadableByPrinPolicy)l;
            return PrincipalUtil.actsFor(rbp.reader, this.reader);
        }
        return false;
    }


    public int hashCode() {
	return (reader==null?0:reader.hashCode());
    }

    public boolean equals(Object o) {
	if (! (o instanceof ReadableByPrinPolicy)) {
	    return false;
	}

	ReadableByPrinPolicy that = (ReadableByPrinPolicy) o;

	return (this.reader == that.reader || 
	        (reader != null && reader.equals(that.reader)
            && that.reader != null && that.reader.equals(reader))); 
    }
    
    public String componentString() {
        return "[readable by " + (reader == null?"<null>":reader.name()) + "]";
    }

}
