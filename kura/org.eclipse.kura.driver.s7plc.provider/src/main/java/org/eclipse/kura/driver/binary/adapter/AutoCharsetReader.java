package org.eclipse.kura.driver.binary.adapter;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class AutoCharsetReader {

    private static final String[] _defaultCharsets = { "US-ASCII", "UTF-8", "GBK", "GB2312", "BIG5", "GB18030" };

    private AutoCharsetReader() {
        throw new IllegalStateException("Utility class");
    }

    public static Charset detectCharset(byte[] bytes) {

        Charset charset = null;

        for (String charsetName : _defaultCharsets) {
            charset = detectCharset(bytes, Charset.forName(charsetName), 0, bytes.length);
            if (charset != null) {
                break;
            }
        }

        return charset;
    }

    public static Charset detectCharset(byte[] bytes, int offset, int length) {

        Charset charset = null;

        for (String charsetName : _defaultCharsets) {
            charset = detectCharset(bytes, Charset.forName(charsetName), offset, length);
            if (charset != null) {
                break;
            }
        }

        return charset;
    }

    public static Charset detectCharset(byte[] bytes, String[] charsets, int offset, int length) {

        Charset charset = null;

        for (String charsetName : charsets) {
            charset = detectCharset(bytes, Charset.forName(charsetName), offset, length);
            if (charset != null) {
                break;
            }
        }

        return charset;
    }

    public static Charset detectCharset(byte[] bytes, String[] charsets) {

        Charset charset = null;

        for (String charsetName : charsets) {
            charset = detectCharset(bytes, Charset.forName(charsetName), 0, bytes.length);
            if (charset != null) {
                break;
            }
        }

        return charset;
    }

    private static Charset detectCharset(byte[] bytes, Charset charset, int offset, int length) {
        try {
            BufferedInputStream input = new BufferedInputStream(new ByteArrayInputStream(bytes, offset, length));

            CharsetDecoder decoder = charset.newDecoder();
            decoder.reset();

            byte[] buffer = new byte[512];
            boolean identified = false;
            while ((input.read(buffer) != -1) && (!identified)) {
                identified = identify(buffer, decoder);
            }

            input.close();

            if (identified) {
                return charset;
            } else {
                return null;
            }

        } catch (Exception e) {
            return null;
        }
    }

    private static boolean identify(byte[] bytes, CharsetDecoder decoder) {
        try {
            decoder.decode(ByteBuffer.wrap(bytes));
        } catch (CharacterCodingException e) {
            return false;
        }
        return true;
    }

    public static Charset getEncoding(String str) {
        if (str == null || str.trim().length() < 1)
            return null;
        for (String encode : _defaultCharsets) {
            try {
                Charset charset = Charset.forName(encode);
                if (str.equals(new String(str.getBytes(charset), charset)))
                    return charset;
            } catch (Exception er) {
            }
        }
        return null;
    }
}
