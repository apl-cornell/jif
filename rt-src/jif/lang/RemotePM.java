package jif.lang;

import java.rmi.*;

public interface RemotePM extends Remote
{
    public boolean remoteActsFor(Principal p1, Principal p2) 
    throws RemoteException;
}
