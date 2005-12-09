package jif.lang;


public interface ConfPolicy {    
    boolean relabelsTo(ConfPolicy p);
    String componentString();
}
