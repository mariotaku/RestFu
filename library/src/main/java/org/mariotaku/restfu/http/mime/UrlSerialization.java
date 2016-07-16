package org.mariotaku.restfu.http.mime;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;

/**
 * Created by mariotaku on 16/7/13.
 */
public abstract class UrlSerialization {

    static final char[] HEX_CHAR_TABLE = {
            '0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'A', 'B',
            'C', 'D', 'E', 'F'
    };

    public static UrlSerialization PATH = new UrlSerialization() {
        final BitSet allowedSet;

        {
            allowedSet = new BitSet(0xFF);

            allowedSet.set('_', true);
            allowedSet.set('-', true);
            allowedSet.set('!', true);
            allowedSet.set('.', true);
            allowedSet.set('~', true);
            allowedSet.set('\'', true);
            allowedSet.set('(', true);
            allowedSet.set(')', true);
            allowedSet.set('*', true);
            for (int i = '0'; i <= '9'; i++) {
                allowedSet.set(i, true);
            }
            for (int i = 'A'; i <= 'Z'; i++) {
                allowedSet.set(i, true);
            }
            for (int i = 'a'; i <= 'z'; i++) {
                allowedSet.set(i, true);
            }
        }

        @Override
        protected void appendEscape(int codePoint, Charset charset, StringBuilder target) {
            if (codePoint <= 0xFF && allowedSet.get(codePoint)) {
                target.appendCodePoint(codePoint);
            } else {
                percentEncode(codePoint, charset, target);
            }
        }
    };

    /**
     * https://url.spec.whatwg.org/#concept-urlsearchparams-list
     */
    public static UrlSerialization QUERY = new UrlSerialization() {
        final BitSet allowedSet;

        {
            allowedSet = new BitSet(0xFF);
            allowedSet.set('*', true);
            allowedSet.set('-', true);
            allowedSet.set('.', true);
            allowedSet.set('_', true);
            for (int i = '0'; i <= '9'; i++) {
                allowedSet.set(i, true);
            }
            for (int i = 'A'; i <= 'Z'; i++) {
                allowedSet.set(i, true);
            }
            for (int i = 'a'; i <= 'z'; i++) {
                allowedSet.set(i, true);
            }
        }

        @Override
        protected void appendEscape(int codePoint, Charset charset, StringBuilder target) {
            if (codePoint == 0x20) {
                target.append('+');
            } else if (codePoint <= 0xFF && allowedSet.get(codePoint)) {
                target.appendCodePoint(codePoint);
            } else {
                percentEncode(codePoint, charset, target);
            }
        }
    };


    protected abstract void appendEscape(int codePoint, Charset charset, StringBuilder target);

    protected final void percentEncode(int codePoint, Charset charset, StringBuilder target) {
        CharBuffer cb = CharBuffer.wrap(Character.toChars(codePoint));
        ByteBuffer encoded = charset.encode(cb);
        for (int i = 0, j = encoded.limit(); i < j; i++) {
            target.append('%');
            byte v = encoded.get(i);
            target.append(HEX_CHAR_TABLE[(v & 0xF0) >>> 4]);
            target.append(HEX_CHAR_TABLE[v & 0xF]);
        }
    }

    public final String serialize(String str, Charset charset) {
        final StringBuilder sb = new StringBuilder();
        serialize(str, charset, sb);
        return sb.toString();
    }

    public final void serialize(String str, Charset charset, StringBuilder target) {
        final int length = str.length();
        for (int offset = 0; offset < length; ) {
            final int codePoint = str.codePointAt(offset);
            appendEscape(codePoint, charset, target);
            offset += Character.charCount(codePoint);
        }
    }

}
