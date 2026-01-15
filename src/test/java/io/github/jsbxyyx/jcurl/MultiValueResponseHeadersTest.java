package io.github.jsbxyyx.jcurl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

/**
 * 测试多值响应头
 */
public class MultiValueResponseHeadersTest {

    @Test
    void test() throws IOException {
        System.out.println("==================== 测试多值响应头 ====================\n");

        // 测试 1: httpbin 的 Set-Cookie
        testSetCookieHeaders();

        // 测试 2: 响应头的所有值
        testAllHeaderValues();

        // 测试 3: Link 头（分页场景）
        testLinkHeaders();
    }

    /**
     * 测试 1: Set-Cookie 响应头
     */
    private static void testSetCookieHeaders() throws IOException {
        System.out.println("【测试 1】Set-Cookie 响应头");
        System.out.println("----------------------------------------");

        // httpbin 的 /cookies/set 端点会设置多个 cookie
        JCurl.HttpResponseModel response = JCurl.create()
                .url("https://httpbin.org/cookies/set")
                .queryParam("cookie1", "value1")
                .queryParam("cookie2", "value2")
                .queryParam("cookie3", "value3")
                .followRedirects(false)  // 不跟随重定向，查看原始响应
                .get()
                .exec();

        System.out.println("状态码:  " + response.getStatusCode());
        
        // 获取所有 Set-Cookie
        List<String> cookies = response.getHeaderValues("set-cookie");
        System.out.println("\nSet-Cookie 数量: " + cookies.size());
        
        for (int i = 0; i < cookies.size(); i++) {
            System.out.println("  [" + i + "] " + cookies.get(i));
        }

        // 验证
        boolean hasMultipleCookies = cookies.size() > 1;
        System.out.println("\n" + (hasMultipleCookies ? "✅" : "❌") + 
                          " 正确获取多个 Set-Cookie 头");

        System.out.println();
    }

    /**
     * 测试 2: 所有响应头的值
     */
    private static void testAllHeaderValues() throws IOException {
        System.out.println("【测试 2】所有响应头");
        System.out.println("----------------------------------------");

        JCurl.HttpResponseModel response = JCurl.create()
                .url("https://httpbin.org/response-headers")
                .queryParam("Custom-Header", "value1")
                .queryParam("Custom-Header", "value2")
                .get()
                .exec();

        System.out.println("状态码: " + response.getStatusCode());
        System.out.println("\n所有响应头:");

        for (java.util.Map.Entry<String, List<String>> entry : response.getHeaders().entrySet()) {
            String headerName = entry.getKey();
            List<String> values = entry.getValue();
            
            if (values. size() == 1) {
                System. out.println("  " + headerName + ": " + values. get(0));
            } else {
                System.out.println("  " + headerName + ": [" + values.size() + " 个值]");
                for (int i = 0; i < values.size(); i++) {
                    System.out.println("    [" + i + "] " + values.get(i));
                }
            }
        }

        System.out.println();
    }

    /**
     * 测试 3: Link 头（GitHub API 风格）
     */
    private static void testLinkHeaders() throws IOException {
        System.out. println("【测试 3】Link 头（分页场景）");
        System.out.println("----------------------------------------");

        // 模拟请求 GitHub API（可能有多个 Link 头）
        JCurl.HttpResponseModel response = JCurl.create()
                .url("https://api.github.com/users")
                .queryParam("per_page", "5")
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .exec();

        System.out.println("状态码: " + response.getStatusCode());

        // 获取 Link 头
        List<String> linkHeaders = response.getHeaderValues("Link");
        
        if (!linkHeaders.isEmpty()) {
            System.out.println("\nLink 头数量: " + linkHeaders.size());
            for (int i = 0; i < linkHeaders.size(); i++) {
                System.out.println("  [" + i + "] " + linkHeaders.get(i));
            }

            // 解析分页链接
            parseLinkHeader(linkHeaders);
        } else {
            System.out. println("\n（此响应没有 Link 头）");
        }

        System.out.println();
    }

    /**
     * 解析 Link 头
     */
    private static void parseLinkHeader(List<String> linkHeaders) {
        System.out.println("\n解析的链接:");
        
        for (String linkHeader : linkHeaders) {
            // Link 格式:  <url>; rel="next", <url>; rel="last"
            String[] links = linkHeader.split(",");
            for (String link : links) {
                link = link.trim();
                if (link.contains("rel=")) {
                    String url = link.substring(link.indexOf('<') + 1, link.indexOf('>'));
                    String rel = link.substring(link. indexOf("rel=\"") + 5, link.lastIndexOf('"'));
                    System.out.println("  " + rel + ": " + url);
                }
            }
        }
    }
}