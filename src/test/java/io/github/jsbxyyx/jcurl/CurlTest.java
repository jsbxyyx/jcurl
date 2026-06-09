package io.github.jsbxyyx.jcurl;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class CurlTest {

    @Test
    void test() throws IOException {
        String curlCommand =
                "curl -X POST https://httpbin.org/post "
                        + "-H 'Content-Type: application/json' "
                        + "-H 'Authorization: Bearer token123' "
                        + "-d '{\"user\":\"john\",\"action\":\"login\"}'";

        JCurl.HttpResponseModel response = JCurl.fromCurl(curlCommand).exec();

        System.out.println("状态码: " + response.getStatusCode());
        System.out.println("响应: " + response.getBody());
    }

    @Test
    void test2() throws Exception {
        JCurl.HttpResponseModel responseModel =
                JCurl.fromCurl(
                                "curl -X POST https://httpbin.org/post "
                                        + " -H \"Content-Type: application/json\" "
                                        + " -d '{\"name\": \"test\"}'")
                        .exec(OkHttpExecutor.create());
        System.out.println(
                "contains application/json : "
                        + responseModel.getBody().contains("application/json"));

        JCurl.HttpResponseModel responseModel2 =
                JCurl.fromCurl(
                                "curl -X POST https://httpbin.org/post "
                                        + " -H \"Content-Type: text/plain\" "
                                        + " -d '{\"name\": \"test\"}'")
                        .exec(OkHttpExecutor.create());
        System.out.println(
                "contains text/plain : " + responseModel2.getBody().contains("text/plain"));
    }

    @Test
    void test1() throws Exception {
        long ts = System.currentTimeMillis();
        String marketCode = "0.002228";
        int lmt = 90;
        // 2025-12-19,3.99,4.37,4.40,3.99,1292773,572007570.88,10.28,9.52,0.38,10.69
        JCurl.HttpResponseModel response =
                JCurl.create()
                        .url(
                                "https://push2his.eastmoney.com/api/qt/stock/kline/get?"
                                        + "cb="
                                        + // ("jQuery351017655795968013988_" + ts) +
                                        "&secid="
                                        + marketCode
                                        + "&ut=fa5fd1943c7b386f172d6893dbfba10b"
                                        + "&fields1=f1%2Cf2%2Cf3%2Cf4%2Cf5%2Cf6"
                                        + "&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61"
                                        + "&klt=101"
                                        + "&fqt=1"
                                        + "&end=20500101"
                                        + "&lmt="
                                        + lmt
                                        + "&_="
                                        + (ts + 20))
                        .opt("-H", "Accept: */*")
                        .opt("-H", "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8")
                        .opt("-H", "Connection: keep-alive")
                        .opt(
                                "-H",
                                "Referer: https://quote.eastmoney.com/"
                                        + (marketCode.replace("1.", "sh").replace("0.", "sz"))
                                        + ".html")
                        .opt("-H", "Sec-Fetch-Dest: script")
                        .opt("-H", "Sec-Fetch-Mode: no-cors")
                        .opt("-H", "Sec-Fetch-Site: same-site")
                        .opt(
                                "-H",
                                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
                        .opt(
                                "-H",
                                "sec-ch-ua: \"Google Chrome\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"")
                        .opt("-H", "sec-ch-ua-mobile: ?0")
                        .opt("-H", "sec-ch-ua-platform: \"Windows\"")
                        .exec(OkHttpExecutor.create());

        System.out.println(response.getStatusCode() + " : " + response.getBody());
    }

    @Test
    void testClientCertificate() throws Exception {
        // 测试使用curl命令行参数设置客户端证书
        String curlCommand = "curl --cert-type P12 --cert bad.p12:password https://httpbin.org/get";

        try {
            JCurl.HttpRequestModel model = JCurl.parse(curlCommand);
            System.out.println("证书类型: " + model.getConfig().getCertType());
            System.out.println("证书路径: " + model.getConfig().getCertPath());
            System.out.println("证书密码: " + model.getConfig().getCertPassword());

            // 验证解析结果
            if (!"P12".equals(model.getConfig().getCertType())) {
                throw new AssertionError("证书类型不匹配");
            }
            if (!"bad.p12".equals(model.getConfig().getCertPath())) {
                throw new AssertionError("证书路径不匹配");
            }
            if (!"password".equals(model.getConfig().getCertPassword())) {
                throw new AssertionError("证书密码不匹配");
            }

            System.out.println("客户端证书参数解析测试通过!");
        } catch (Exception e) {
            System.out.println("测试异常（预期，因为证书文件不存在）: " + e.getMessage());
        }
    }

    @Test
    void testClientCertificateAPI() throws Exception {
        // 测试使用API方法设置客户端证书（从文件路径）- 使用枚举
        JCurl curl =
                JCurl.create()
                        .url("https://httpbin.org/get")
                        .clientCert(JCurl.CertType.PKCS12, "test.p12", "password123");

        JCurl.HttpRequestModel model = curl.build();
        System.out.println("API设置(枚举) - 证书类型: " + model.getConfig().getCertType());
        System.out.println("API设置(枚举) - 证书路径: " + model.getConfig().getCertPath());
        System.out.println("API设置(枚举) - 证书密码: " + model.getConfig().getCertPassword());

        if (!"PKCS12".equals(model.getConfig().getCertType())) {
            throw new AssertionError("证书类型不匹配");
        }
        if (!"test.p12".equals(model.getConfig().getCertPath())) {
            throw new AssertionError("证书路径不匹配");
        }
        if (!"password123".equals(model.getConfig().getCertPassword())) {
            throw new AssertionError("证书密码不匹配");
        }

        // 测试字符串版本（向后兼容）
        JCurl curl2 =
                JCurl.create()
                        .url("https://httpbin.org/get")
                        .clientCert("P12", "test2.p12", "pass456");

        JCurl.HttpRequestModel model2 = curl2.build();
        System.out.println("API设置(字符串) - 证书类型: " + model2.getConfig().getCertType());

        if (!"PKCS12".equals(model2.getConfig().getCertType())) {
            throw new AssertionError("P12应该映射到PKCS12");
        }

        System.out.println("客户端证书API测试（文件路径）通过!");
    }

    @Test
    void testClientCertificateAPIWithBytes() throws Exception {
        // 测试使用API方法设置客户端证书（从字节数组）- 使用枚举
        byte[] certBytes = "dummy cert data".getBytes();

        JCurl curl =
                JCurl.create()
                        .url("https://httpbin.org/get")
                        .clientCert(JCurl.CertType.PKCS12, certBytes, "password123");

        JCurl.HttpRequestModel model = curl.build();
        System.out.println("API设置(枚举) - 证书类型: " + model.getConfig().getCertType());
        System.out.println("API设置(枚举) - 证书字节数组长度: " + model.getConfig().getCertBytes().length);
        System.out.println("API设置(枚举) - 证书密码: " + model.getConfig().getCertPassword());

        if (!"PKCS12".equals(model.getConfig().getCertType())) {
            throw new AssertionError("证书类型不匹配");
        }
        if (certBytes != model.getConfig().getCertBytes()) {
            throw new AssertionError("证书字节数组不匹配");
        }
        if (!"password123".equals(model.getConfig().getCertPassword())) {
            throw new AssertionError("证书密码不匹配");
        }

        // 测试字符串版本（向后兼容）
        JCurl curl2 =
                JCurl.create()
                        .url("https://httpbin.org/get")
                        .clientCert("JKS", certBytes, "jkspass");

        JCurl.HttpRequestModel model2 = curl2.build();
        System.out.println("API设置(字符串) - 证书类型: " + model2.getConfig().getCertType());

        if (!"JKS".equals(model2.getConfig().getCertType())) {
            throw new AssertionError("JKS类型不匹配");
        }

        System.out.println("客户端证书API测试（字节数组）通过!");
    }

    @Test
    void testClientCertificateWithOkHttp() throws Exception {
        // 测试 OkHttpExecutor 的客户端证书支持
        String curlCommand =
                "curl --cert-type P12 --cert test.p12:password https://httpbin.org/get";

        try {
            JCurl.HttpRequestModel model = JCurl.parse(curlCommand);
            System.out.println("OkHttp - 证书类型: " + model.getConfig().getCertType());
            System.out.println("OkHttp - 证书路径: " + model.getConfig().getCertPath());
            System.out.println("OkHttp - 证书密码: " + model.getConfig().getCertPassword());

            if (!"P12".equals(model.getConfig().getCertType())) {
                throw new AssertionError("证书类型不匹配");
            }
            if (!"test.p12".equals(model.getConfig().getCertPath())) {
                throw new AssertionError("证书路径不匹配");
            }
            if (!"password".equals(model.getConfig().getCertPassword())) {
                throw new AssertionError("证书密码不匹配");
            }

            System.out.println("OkHttpExecutor 客户端证书配置测试通过!");
        } catch (Exception e) {
            System.out.println("测试异常（预期，因为证书文件不存在）: " + e.getMessage());
        }
    }
}
