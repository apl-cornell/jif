package jif.lang;

public class InternalPM implements PrincipalManager
{
    public boolean actsFor(Principal p1, Principal p2) {
	return p1.actsFor(p2);
    }
}
