package jif.lang;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

public class RemotePM_c extends UnicastRemoteObject 
	implements PrincipalManager, RemotePM
{
    Map actsForMap; // principal -> set(principal)

    public RemotePM_c() throws RemoteException {
	this.actsForMap = new HashMap(100);
    }

    private boolean search(Principal p1, Principal p2) {
	Set actsForSet = (Set) actsForMap.get(p1);
	
	if (actsForSet != null && actsForSet.contains(p2)) 
	    return true;
	
	return false;
    }

    public void grant(Principal p1, Principal p2) {
	Set actsForSet = (Set) actsForMap.get(p1);

	if (actsForSet == null) {
	    actsForSet = new HashSet();
	    actsForMap.put(p1, actsForSet);
	}
	
	actsForSet.add(p2);
    }

    public void revoke(Principal p1, Principal p2) {
	Set actsForSet = (Set) actsForMap.get(p1);

	if (actsForSet != null) 
	    actsForSet.remove(p2);
    }

    public boolean actsFor(Principal p1, Principal p2) {
	try {
	    return remoteActsFor(p1, p2); 
	}
	catch (RemoteException x) {
	    x.printStackTrace();
	    return false;
	}
    }
    
    public boolean remoteActsFor(Principal p1, Principal p2) 
    throws RemoteException {
	if (search(p1, p2) == true) return true;
	
	if (p1 instanceof NativePrincipal && p2 instanceof NativePrincipal)
	    return NativePM.actsFor((NativePrincipal)p1, (NativePrincipal)p2);

	return false;		
    }
}
    
