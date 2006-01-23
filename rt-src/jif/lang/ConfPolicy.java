package jif.lang;


public interface ConfPolicy extends Policy {    
    ConfPolicy join(ConfPolicy p);
    ConfPolicy meet(ConfPolicy p);
}
