package jif.lang;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class NativePrincipal extends AbstractPrincipal
{
    private String name;
    private String sid;
    private String domain;
    private String fullName;
    private PrincipalManager pm = null;
    private boolean pmInit = false;

    protected static final String PRINCIPAL_MANAGER_URL = 
                                 "//localhost/PrincipalManager";
                                 
    public NativePrincipal(String domain, String name, String sid) {
	this.domain = domain;
	this.name = name;
	this.sid = sid;
	this.fullName = (domain != null? (domain + "\\") : "") + name;
    }

    public NativePrincipal(String domain, String name) {
	this(domain, name, null);
    }
    
    public NativePrincipal(String fullName) {
	this.sid = null;
	int index = fullName.indexOf('\\');
	if (index == -1) { 
	    this.domain = null;
	    this.name = fullName;
	}
	else {
	    this.domain = fullName.substring(0, index);
	    this.name = fullName.substring(index + 1);
	}
	this.fullName = fullName;
    }

    public String name() {
	return name;
    }

    public String domain() {
	return domain;
    }

    public String fullName() {
	return fullName;
    }

    public String sid() {
	return sid;
    }

    public void setSid(String sid) {
	this.sid = sid;
    }

    protected boolean actsForImpl(Principal p) { 
        if (this.equals(p)) return true;
	
        if (!pmInit && pm == null) {
            pmInit = true;
	   try {
		pm = (PrincipalManager) Naming.lookup(PRINCIPAL_MANAGER_URL);
            }
            catch (NotBoundException x) {
                pm = null;
            }
            catch (RemoteException x) {
                pm = null;
            }
            catch (java.net.MalformedURLException x) {
                throw new RuntimeException(x.getMessage());
            }
        }

        if (pm == null) {
            return false;
        }

        return pm.actsFor(this, p);
    }

    public boolean equals(Object obj) {
	if (! (obj instanceof NativePrincipal)) return false;

	NativePrincipal that = (NativePrincipal) obj;
	return this.fullName().equals(that.fullName());
    }

    public int hashCode() {
	return this.fullName().hashCode();
    }
}
