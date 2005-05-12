package jif.lang;

public abstract class Principal_JIF_IMPL {
    public static boolean jif$Instanceof(final Object o) {
        if (o instanceof Principal) { Principal c = (Principal) o; }
        return false;
    }
    
    public static Principal jif$cast$jif_lang_Principal(final Object o) {
        if (jif$Instanceof(o)) return (Principal) o;
        throw new ClassCastException();
    }
    
    public Principal_JIF_IMPL() { super(); }
}
