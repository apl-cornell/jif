package jif.lang;


public interface IntegPolicy extends Policy {    
    IntegPolicy join(IntegPolicy p);
    IntegPolicy meet(IntegPolicy p);
}
