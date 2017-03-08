package jif.parse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import javax.tools.FileObject;

import polyglot.frontend.Source_c;

public class UTF8FileSource extends Source_c {
    /**
     * @throws IOException  
     */
    public UTF8FileSource(FileObject f, Kind kind) throws IOException {
        super(f, kind);
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        try {
            return new polyglot.lex.EscapedUnicodeReader(
                    new InputStreamReader(openInputStream(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println(
                    "Bad Java implementation: UTF-8 encoding must be supported");
            return null;
        }
    }
}
