package jif.lang;

public class PMFactory
{
    public static PrincipalManager create() {
	return create("internal");
    }
    
    public static PrincipalManager create(String impl) {
	PrincipalManager pm = null;
	
	if (impl == null || impl.equals("internal")) 
	    pm = new InternalPM();

	return pm;
    }
}
