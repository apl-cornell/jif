package jif.lang;


public interface IntegPolicy {    
    boolean relabelsTo(IntegPolicy p);
    String componentString();
}
