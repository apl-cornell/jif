package jif.lang;

public class PTFactory
{
    public static PrincipalTranslator create() {
	return create("default");
    }
    
    public static PrincipalTranslator create(String alg) {
	PrincipalTranslator pt = null;
	if (alg == null || alg.equals("default")) 
	    pt = null; //TODO

	return pt;
    }
}
