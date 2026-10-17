package io.github.jsbxyyx.jcurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ExplicitRequestMethodTest {

    @Test
    void explicitGetWithDataKeepsGetMethodAndSendsBody() {
        JCurl.HttpRequestModel request =
                JCurl.parse(new String[] {"curl", "-X", "GET", "-d", "a=1", "https://example.com"});

        assertEquals("GET", request.getMethod());
        assertEquals("a=1", request.getBody());
        assertNull(request.getBinaryBody());
    }

    @Test
    void explicitGetWithDataKeepsGetMethodWhenDataComesFirst() {
        JCurl.HttpRequestModel request =
                JCurl.parse(new String[] {"curl", "-d", "a=1", "-X", "GET", "https://example.com"});

        assertEquals("GET", request.getMethod());
        assertEquals("a=1", request.getBody());
    }

    @Test
    void explicitPutWithDataKeepsPutMethodAndSendsBody() {
        JCurl.HttpRequestModel request =
                JCurl.parse(new String[] {"curl", "-X", "PUT", "-d", "a=1", "https://example.com"});

        assertEquals("PUT", request.getMethod());
        assertEquals("a=1", request.getBody());
        assertNull(request.getBinaryBody());
    }

    @Test
    void getModeMovesDataIntoQueryAndLeavesBodyEmpty() {
        JCurl.HttpRequestModel request =
                JCurl.parse(new String[] {"curl", "-G", "-d", "a=1", "https://example.com"});

        assertEquals("GET", request.getMethod());
        assertNull(request.getBody());
        assertEquals("https://example.com?a=1", request.getFullUrl());
    }

    @Test
    void dataWithoutExplicitMethodStillDefaultsToPost() {
        JCurl.HttpRequestModel request =
                JCurl.parse(new String[] {"curl", "-d", "a=1", "https://example.com"});

        assertEquals("POST", request.getMethod());
        assertEquals("a=1", request.getBody());
    }
}
