package io.github.jsbxyyx.jcurl;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class CurlTest {

    @Test
    void test() throws IOException {
        String curlCommand = "curl -X POST https://httpbin.org/post " +
                "-H 'Content-Type: application/json' " +
                "-H 'Authorization: Bearer token123' " +
                "-d '{\"user\":\"john\",\"action\":\"login\"}'";

        JCurl opt = JCurl.fromCurl(curlCommand);

        JCurl.HttpResponseModel response = opt.exec();
        System.out.println("状态码: " + response.getStatusCode() + "\n响应: " + response.getBody());

        response = opt.exec(OkHttpExecutor.create());
        System.out.println("状态码: " + response.getStatusCode() + "\n响应: " + response.getBody());

        response = opt.exec(HttpClient4Executor.create());
        System.out.println("状态码: " + response.getStatusCode() + "\n响应: " + response.getBody());
    }

    @Test
    void test2() throws Exception {
        {
            JCurl opt = JCurl.fromCurl("curl -X POST https://httpbin.org/post " +
                    " -H \"Content-Type: application/json\" " +
                    " -d '{\"name\": \"test\"}'");
            JCurl.HttpResponseModel responseModel = opt.exec();
            System.out.println("contains Content-Type: application/json : " + responseModel.getBody().contains("application/json"));

            responseModel = opt.exec(OkHttpExecutor.create());
            System.out.println("contains Content-Type: application/json : " + responseModel.getBody().contains("application/json"));

            responseModel = opt.exec(HttpClient4Executor.create());
            System.out.println("contains Content-Type: application/json : " + responseModel.getBody().contains("application/json"));
        }
        {
            JCurl opt = JCurl.fromCurl("curl -X POST https://httpbin.org/post " +
                            " -H \"Content-Type: text/plain\" " +
                            " -d '{\"name\": \"test\"}'");
            JCurl.HttpResponseModel responseModel = opt.exec();
            System.out.println("contains Content-Type: text/plain : " + responseModel.getBody().contains("text/plain"));

            responseModel = opt.exec(OkHttpExecutor.create());
            System.out.println("contains Content-Type: text/plain : " + responseModel.getBody().contains("text/plain"));

            responseModel = opt.exec(HttpClient4Executor.create());
            System.out.println("contains Content-Type: text/plain : " + responseModel.getBody().contains("text/plain"));
        }
        {
            JCurl opt = JCurl.fromCurl("curl -X POST https://httpbin.org/post " +
                            " -H \"Content-Type: application/json\" " +
                            " -H \"Origin: http://localhost\" " +
                            " -d '{\"name\": \"test\"}'");
            JCurl.HttpResponseModel responseModel = opt.exec();
            System.out.println("contains Origin: http://localhost : " + responseModel.getBody().contains("http://localhost"));

            responseModel = opt.exec(OkHttpExecutor.create());
            System.out.println("contains Origin: http://localhost : " + responseModel.getBody().contains("http://localhost"));

            responseModel = opt.exec(HttpClient4Executor.create());
            System.out.println("contains Origin: http://localhost : " + responseModel.getBody().contains("http://localhost"));
        }
        {
            // gzip
            JCurl opt = JCurl.fromCurl("curl -X GET https://httpbin.org/gzip " +
                    " -H \"Accept-Encoding: gzip\"");
            JCurl.HttpResponseModel responseModel = opt.exec();
            System.out.println("contains gzipped : " + responseModel.getBody().contains("\"gzipped\": true"));

            responseModel = opt.exec(OkHttpExecutor.create());
            System.out.println("contains gzipped : " + responseModel.getBody().contains("\"gzipped\": true"));

            responseModel = opt.exec(HttpClient4Executor.create());
            System.out.println("contains gzipped : " + responseModel.getBody().contains("\"gzipped\": true"));
        }
        {
            // deflate
            JCurl opt = JCurl.fromCurl("curl -X GET https://httpbin.org/deflate " +
                    " -H \"Accept-Encoding: deflate\"");
            JCurl.HttpResponseModel responseModel = opt.exec();
            System.out.println("contains deflated : " + responseModel.getBody().contains("\"deflated\": true"));

            responseModel = opt.exec(OkHttpExecutor.create());
            System.out.println("contains deflated : " + responseModel.getBody().contains("\"deflated\": true"));

            responseModel = opt.exec(HttpClient4Executor.create());
            System.out.println("contains deflated : " + responseModel.getBody().contains("\"deflated\": true"));
        }
    }

    @Test
    void test1() throws Exception {
        long ts = System.currentTimeMillis();
        String marketCode = "0.002228";
        int lmt = 90;
        // 2025-12-19,3.99,4.37,4.40,3.99,1292773,572007570.88,10.28,9.52,0.38,10.69
        JCurl opt = JCurl.create()
                .url("https://push2his.eastmoney.com/api/qt/stock/kline/get?" + "cb="
                        + // ("jQuery351017655795968013988_" + ts) +
                        "&secid="
                        + marketCode + "&ut=fa5fd1943c7b386f172d6893dbfba10b"
                        + "&fields1=f1%2Cf2%2Cf3%2Cf4%2Cf5%2Cf6"
                        + "&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61"
                        + "&klt=101"
                        + "&fqt=1"
                        + "&end=20500101"
                        + "&lmt="
                        + lmt + "&_="
                        + (ts + 20))
                .opt("-H", "Accept: */*")
                .opt("-H", "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8")
                .opt("-H", "Connection: keep-alive")
                .opt("-H", "Referer: https://quote.eastmoney.com/" + (marketCode.replace("1.", "sh").replace("0.", "sz")) + ".html")
                .opt("-H", "Sec-Fetch-Dest: script")
                .opt("-H", "Sec-Fetch-Mode: no-cors")
                .opt("-H", "Sec-Fetch-Site: same-site")
                .opt("-H", "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
                .opt("-H", "sec-ch-ua: \"Google Chrome\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"")
                .opt("-H", "sec-ch-ua-mobile: ?0")
                .opt("-H", "sec-ch-ua-platform: \"Windows\"");

        JCurl.HttpResponseModel response = opt.exec();
        System.out.println(response.getStatusCode() + " : " + response.getBody());

        response = opt.exec(OkHttpExecutor.create());
        System.out.println(response.getStatusCode() + " : " + response.getBody());

        response = opt.exec(HttpClient4Executor.create());
        System.out.println(response.getStatusCode() + " : " + response.getBody());
    }

}
