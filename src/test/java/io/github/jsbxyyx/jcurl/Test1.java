package io.github.jsbxyyx.jcurl;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Test1 {

    @Test
    void queryParamTreatsInputsAsRawValuesAndEncodesWhenBuildingUrl() {
        JCurl.HttpRequestModel request = new JCurl.HttpRequestModel();
        request.setUrl("https://example.com/search");
        request.addQueryParam("keyword", "hello world");
        request.addQueryParam("path", "%2Fdocs%2F");

        assertEquals(
                "https://example.com/search?keyword=hello+world&path=%252Fdocs%252F",
                request.getFullUrl());
    }

    @Test
    void curlMultipleDataOptionsAreJoinedForPost() {
        JCurl.HttpRequestModel request = JCurl.parse(
                "curl 'https://example.com/api' -d 'name=Tom' -d 'age=18'");

        assertEquals("POST", request.getMethod());
        assertEquals("name=Tom&age=18", request.getBody());
    }

    @Test
    void curlMultipleDataOptionsDoNotMergeJsonObjects() {
        JCurl.HttpRequestModel request = JCurl.parse(
                "curl -H 'Content-Type: application/json' -d '{\"name\":\"Tom\"}' -d '{\"age\":18}' https://example.com/api");

        assertEquals("{\"name\":\"Tom\"}&{\"age\":18}", request.getBody());
        assertEquals("application/json", request.getHeader("Content-Type"));
    }

    @Test
    void curlMultipleDataOptionsAreJoinedForExplicitPutMethod() {
        JCurl.HttpRequestModel request = JCurl.parse(
                "curl -X PUT https://example.com/api -d 'name=Tom' -d 'age=18'");

        assertEquals("PUT", request.getMethod());
        assertEquals("name=Tom&age=18", request.getBody());
    }

    @Test
    void curlGetDataOptionsAreJoinedWhenGetOptionComesFirstOrLast() {
        JCurl.HttpRequestModel getFirst = JCurl.parse(
                "curl -G -d 'a=1' -d 'b=2' https://example.com/api");
        JCurl.HttpRequestModel getLast = JCurl.parse(
                "curl -d 'a=1' -d 'b=2' -G https://example.com/api");

        assertEquals("GET", getFirst.getMethod());
        assertNull(getFirst.getBody());
        assertEquals("https://example.com/api?a=1&b=2", getFirst.getFullUrl());

        assertEquals("GET", getLast.getMethod());
        assertNull(getLast.getBody());
        assertEquals("https://example.com/api?a=1&b=2", getLast.getFullUrl());
    }

    @Test
    void explicitRequestMethodTakesPrecedenceOverGetOptionRegardlessOfOrder() {
        JCurl.HttpRequestModel getThenPut = JCurl.parse(
                "curl -G -X PUT -d 'a=1' https://example.com/api");
        JCurl.HttpRequestModel putThenGet = JCurl.parse(
                "curl -X PUT -G -d 'a=1' https://example.com/api");

        assertEquals("PUT", getThenPut.getMethod());
        assertEquals("https://example.com/api?a=1", getThenPut.getFullUrl());
        assertNull(getThenPut.getBody());
        assertEquals("PUT", putThenGet.getMethod());
        assertEquals("https://example.com/api?a=1", putThenGet.getFullUrl());
        assertNull(putThenGet.getBody());

        JCurl.HttpRequestModel builderRequest = JCurl.create()
                .url("https://example.com/api")
                .opt("-G")
                .opt("-X", "PUT")
                .build();
        assertEquals("PUT", builderRequest.getMethod());
    }

    @Test
    void getModeMovesDataBinaryIntoQueryRegardlessOfOptionOrder() {
        JCurl.HttpRequestModel getFirst = JCurl.parse(
                "curl -G --data-binary 'a=1&b=2' https://example.com/api");
        JCurl.HttpRequestModel getLast = JCurl.parse(
                "curl --data-binary 'a=1&b=2' -G https://example.com/api");

        for (JCurl.HttpRequestModel request : Arrays.asList(getFirst, getLast)) {
            assertEquals("GET", request.getMethod());
            assertNull(request.getBody());
            assertNull(request.getBinaryBody());
            assertEquals("https://example.com/api?a=1&b=2", request.getFullUrl());
        }

        JCurl.HttpRequestModel builderRequest = JCurl.create()
                .url("https://example.com/api")
                .opt("--data-binary", "a=1&b=2")
                .opt("-G")
                .build();
        assertEquals("GET", builderRequest.getMethod());
        assertNull(builderRequest.getBinaryBody());
        assertEquals("https://example.com/api?a=1&b=2", builderRequest.getFullUrl());
    }

    @Test
    void builderJoinsMultipleDataOptions() {
        JCurl.HttpRequestModel request = JCurl.create()
                .url("https://example.com/api")
                .opt("-d", "a=1")
                .opt("-d", "b=2")
                .build();

        assertEquals("POST", request.getMethod());
        assertEquals("a=1&b=2", request.getBody());
    }

    @Test
    void curlGetDataPreservesEncodedQueryDataAndExistingUrlQuery() {
        JCurl.HttpRequestModel request = JCurl.parse(
                "curl -G --data 'path=%2Fdocs%2F&name=hello%20world' 'https://example.com/search?existing=%2F'");

        assertEquals(
                "https://example.com/search?existing=%2F&path=%2Fdocs%2F&name=hello%20world",
                request.getFullUrl());
    }

    @Test
    void curlGetDataWorksWhenDataOptionAppearsBeforeGetOption() {
        JCurl.HttpRequestModel request = JCurl.parse(
                "curl --data 'path=%2Fdocs%2F' -G 'https://example.com/search?existing=%2F'");

        assertEquals(
                "https://example.com/search?existing=%2F&path=%2Fdocs%2F",
                request.getFullUrl());
        assertEquals("GET", request.getMethod());
    }

    @Test
    void curlDataUrlencodeEncodesOnlyTheValueAfterEquals() {
        JCurl.HttpRequestModel request = JCurl.parse(
                "curl -G --data-urlencode 'name=hello world' 'https://example.com/search?existing=%2F'");

        assertEquals(
                "https://example.com/search?existing=%2F&name=hello+world",
                request.getFullUrl());
    }

    @Test
    void queryParamsAreInsertedBeforeUrlFragment() {
        JCurl.HttpRequestModel request = new JCurl.HttpRequestModel();
        request.setUrl("https://example.com/search#section");
        request.addQueryParam("q", "java");

        assertEquals("https://example.com/search?q=java#section", request.getFullUrl());
    }

    @Test
    void queryParamsPreserveExistingEncodedQueryAndFragment() {
        JCurl.HttpRequestModel request = new JCurl.HttpRequestModel();
        request.setUrl("https://example.com/search?path=%2Fdocs%2F&name=hello%20world&tag=a&tag=b#results");
        request.addQueryParam("q", "中文");

        assertEquals(
                "https://example.com/search?path=%2Fdocs%2F&name=hello%20world&tag=a&tag=b&q=%E4%B8%AD%E6%96%87#results",
                request.getFullUrl());
    }

    @Test
    void queryParamsRejectMalformedUriWhenParametersNeedToBeAdded() {
        JCurl.HttpRequestModel request = new JCurl.HttpRequestModel();
        request.setUrl("https://example.com/a path");
        request.addQueryParam("q", "java");

        assertThrows(IllegalArgumentException.class, request::getFullUrl);
    }

    @Test
    void queryParamsDoNotAddExtraSeparatorWhenUrlEndsWithQuestionMarkOrAmpersand() {
        JCurl.HttpRequestModel questionMarkRequest = new JCurl.HttpRequestModel();
        questionMarkRequest.setUrl("https://example.com/search?");
        questionMarkRequest.addQueryParam("q", "java");
        assertEquals("https://example.com/search?q=java", questionMarkRequest.getFullUrl());

        JCurl.HttpRequestModel ampersandRequest = new JCurl.HttpRequestModel();
        ampersandRequest.setUrl("https://example.com/search?lang=en&");
        ampersandRequest.addQueryParam("q", "java");
        assertEquals("https://example.com/search?lang=en&q=java", ampersandRequest.getFullUrl());
    }

    @Test
    void setHeadersNormalizesExistingMixedCaseKeys() {
        JCurl.HttpRequestModel request = new JCurl.HttpRequestModel();
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        request.setHeaders(headers);

        assertEquals("application/json", request.getHeader("content-type"));
        assertEquals("application/json", request.getHeader("CONTENT-TYPE"));
    }

    @Test
    void setFormFieldWorksBeforeAnyFormFieldsWereAdded() {
        JCurl.HttpRequestModel request = new JCurl.HttpRequestModel();

        request.setFormField("name", JCurl.HttpRequestModel.FormField.text("value"));

        assertEquals("value", request.getFormField("name").getValue());
    }

    @Test
    void downloadLimitFailureReturnsEmptyBodyAndKeepsHttpStatus() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] responseBody = "this body exceeds the configured limit".getBytes(StandardCharsets.UTF_8);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();

        try {
            JCurl.HttpRequestModel request = new JCurl.HttpRequestModel();
            request.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/");
            request.getConfig().setMaxDownloadSize(4);

            JCurl.HttpResponseModel response = JCurl.HttpUrlConnectionExecutor.create().execute(request);
            assertEquals(200, response.getStatusCode());
            assertArrayEquals(new byte[0], response.getBodyBytes());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void caseInsensitiveHeadersDoNotDependOnDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            JCurl.HttpRequestModel request = new JCurl.HttpRequestModel();
            request.setHeader("CONTENT-TYPE", "application/json");

            assertEquals("application/json", request.getHeader("content-type"));
        } finally {
            Locale.setDefault(original);
        }
    }
}
