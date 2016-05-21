package org.mariotaku.restfu.oauth;

import okio.ByteString;

/**
 * Created by mariotaku on 16/5/21.
 */
class Base64 {

    static Platform platform = Platform.get();

    static String encodeNoWrap(byte[] data) {
        return platform.encodeNoWrap(data);
    }

    static abstract class Platform {

        public static Platform get() {
            try {
                Class.forName("android.util.Base64");
                return new Android();
            } catch (Exception e) {
                return new OkioPlatform();
            }
        }

        abstract String encodeNoWrap(byte[] data);
    }

    static class Android extends Platform {

        @Override
        String encodeNoWrap(byte[] data) {
            return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP);
        }
    }

    static class OkioPlatform extends Platform {

        @Override
        String encodeNoWrap(byte[] data) {
            return ByteString.of(data).base64Url();
        }
    }
}
