package jif.parse;

import java.io.*;
import java.util.Date;

import javax.tools.JavaFileObject;

import polyglot.frontend.FileSource;
import polyglot.frontend.Source_c;

public class UTF8FileSource extends Source_c {
    /**
     * @throws IOException  
     */
    public UTF8FileSource(JavaFileObject f, boolean userSpecified) throws IOException {
	super(f, userSpecified);
    }
    
    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        try {
            return new polyglot.lex.EscapedUnicodeReader(
                new InputStreamReader(openInputStream(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println("Bad Java implementation: UTF-8 encoding must be supported");
            return null;
        }
    }
}
