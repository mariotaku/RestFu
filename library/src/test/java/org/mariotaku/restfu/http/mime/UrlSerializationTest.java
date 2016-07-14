package org.mariotaku.restfu.http.mime;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

/**
 * Created by mariotaku on 16/7/13.
 */
public class UrlSerializationTest {

    @org.junit.Test
    public void testSerialize() throws Exception {
        assertEquals("ABC+%2B", UrlSerialization.QUERY.serialize("ABC +", Charset.forName("UTF-8")));
    }
}