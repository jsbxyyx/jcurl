package io.github.jsbxyyx.jcurl;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static io.github.jsbxyyx.jcurl.JCurl.Constants.APPLICATION_OCTET_STREAM_VALUE;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.APPLICATION_X_WWW_FORM_URLENCODED_VALUE;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.AUTHORIZATION;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.BASIC_SPACE;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.CONTENT_ENCODING;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.CONTENT_TYPE;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.COOKIE;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.DEFLATE_VALUE;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.GZIP_VALUE;

/**
 * Apache HttpClient4实现的HTTP请求执行器
 * 需要依赖: org.apache.httpcomponents:httpclient:4.5.x
 */
public class HttpClient4Executor implements JCurl.HttpExecutor {

    private static final HttpClient4Executor executor = new HttpClient4Executor();

    private HttpClient4Executor() {
    }

    public static HttpClient4Executor create() {
        return executor;
    }

    @Override
    public JCurl.HttpResponseModel execute(JCurl.HttpRequestModel requestModel) throws IOException {
        CloseableHttpClient client = buildClient(requestModel);
        HttpRequestBase request = buildRequest(requestModel);

        try {
            // 执行请求（带重试）
            return executeWithRetry(
                    client,
                    request,
                    requestModel);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static CloseableHttpClient buildClient(JCurl.HttpRequestModel requestModel) {
        HttpClientBuilder builder = HttpClients.custom();

        // 请求配置
        RequestConfig.Builder configBuilder = RequestConfig.custom()
                .setConnectTimeout((int) requestModel.getConfig().getConnectTimeout())
                .setSocketTimeout((int) requestModel.getConfig().getReadTimeout())
                .setConnectionRequestTimeout((int) requestModel.getConfig().getReadTimeout());

        // 代理设置
        if (requestModel.getConfig().getProxy() != null) {
            // HttpClient4不支持在此处设置代理，需要在请求执行时设置
            // 这里仅保留接口一致性
        } else if (requestModel.getConfig().getProxyHost() != null
                && requestModel.getConfig().getProxyPort() > 0) {
            // 代理设置也需要在执行时处理
        }

        RequestConfig requestConfig = configBuilder.build();
        builder.setDefaultRequestConfig(requestConfig);

        // 重定向策略
        if (requestModel.getConfig().isFollowRedirects()) {
            builder.setRedirectStrategy(new DefaultRedirectStrategy());
        }

        // SSL验证
        if (!requestModel.getConfig().isVerifySSL()) {
            try {
                TrustManager[] trustManagers = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                };

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagers, new java.security.SecureRandom());

                SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                        sslContext,
                        NoopHostnameVerifier.INSTANCE);
                builder.setSSLSocketFactory(socketFactory);
            } catch (Exception e) {
                throw new RuntimeException("config SSL failed.", e);
            }
        }

        return builder.build();
    }

    private static HttpRequestBase buildRequest(JCurl.HttpRequestModel requestModel) throws IOException {
        String method = requestModel.getMethod();
        String url = requestModel.getFullUrl();

        HttpRequestBase request;

        // 创建对应方法的请求对象
        switch (method.toUpperCase()) {
            case "GET":
                request = new HttpGet(url);
                break;
            case "POST":
                request = new HttpPost(url);
                break;
            case "PUT":
                request = new HttpPut(url);
                break;
            case "DELETE":
                request = new HttpDelete(url);
                break;
            case "PATCH":
                request = new HttpPatch(url);
                break;
            case "HEAD":
                request = new HttpHead(url);
                break;
            case "OPTIONS":
                request = new HttpOptions(url);
                break;
            case "TRACE":
                request = new HttpTrace(url);
                break;
            default:
                throw new IllegalArgumentException("Unsupported method: " + method);
        }

        // 添加请求头（支持多值）
        for (Map.Entry<String, List<String>> header : requestModel.getHeaders().entrySet()) {
            for (String value : header.getValue()) {
                request.addHeader(header.getKey(), value);
            }
        }

        // 添加Cookie
        if (!requestModel.getCookies().isEmpty()) {
            StringBuilder cookieHeader = new StringBuilder();
            for (Map.Entry<String, String> cookie : requestModel.getCookies().entrySet()) {
                if (cookieHeader.length() > 0)
                    cookieHeader.append("; ");
                cookieHeader.append(cookie.getKey()).append("=").append(cookie.getValue());
            }
            request.addHeader(COOKIE, cookieHeader.toString());
        }

        // Basic认证
        if (requestModel.getUsername() != null) {
            String credentials = requestModel.getUsername() + ":"
                    + (requestModel.getPassword() != null ? requestModel.getPassword() : "");
            String encodedCredentials = JCurl.b64Encode(credentials.getBytes(StandardCharsets.UTF_8));
            request.addHeader(AUTHORIZATION, BASIC_SPACE + encodedCredentials);
        }

        // 构建请求体
        if (request instanceof HttpPost
                || request instanceof HttpPut
                || request instanceof HttpPatch
                || request instanceof HttpDelete) {
            HttpEntity entity = buildRequestEntity(requestModel);
            if (entity != null) {
                if (request instanceof HttpPost) {
                    ((HttpPost) request).setEntity(entity);
                } else if (request instanceof HttpPut) {
                    ((HttpPut) request).setEntity(entity);
                } else if (request instanceof HttpPatch) {
                    ((HttpPatch) request).setEntity(entity);
                }
            }
        }

        return request;
    }

    private static HttpEntity buildRequestEntity(JCurl.HttpRequestModel requestModel) throws IOException {
        // 无请求体
        if (requestModel.getBody() == null
                && requestModel.getBinaryBody() == null
                && requestModel.getFormFields() == null) {
            return null;
        }

        // Multipart表单
        if (requestModel.getFormFields() != null) {
            MultipartEntityBuilder multipartBuilder = MultipartEntityBuilder.create();

            for (Map.Entry<String, JCurl.HttpRequestModel.FormField> entry : requestModel.getAllFormFields()) {
                JCurl.HttpRequestModel.FormField field = entry.getValue();

                if (field.isFile()) {
                    File file = new File(field.getFilePath());
                    multipartBuilder.addBinaryBody(entry.getKey(),
                            file,
                            org.apache.http.entity.ContentType.parse(field.getContentType() != null
                                    ? field.getContentType()
                                    : APPLICATION_OCTET_STREAM_VALUE),
                            field.getFileName());
                } else {
                    multipartBuilder.addTextBody(entry.getKey(), field.getValue());
                }
            }

            return multipartBuilder.build();
        }

        // 二进制数据
        if (requestModel.getBinaryBody() != null) {
            ByteArrayEntity entity = new ByteArrayEntity(requestModel.getBinaryBody());
            String contentType = getContentType(requestModel.getHeaders());
            if (contentType != null) {
                entity.setContentType(contentType);
            }
            return entity;
        }

        // 文本数据
        if (requestModel.getBody() != null) {
            String contentType = getContentType(requestModel.getHeaders());
            if (contentType == null) {
                contentType = APPLICATION_X_WWW_FORM_URLENCODED_VALUE;
            }
            return new StringEntity(requestModel.getBody(), contentType, StandardCharsets.UTF_8.name());
        }

        return null;
    }

    private static String getContentType(Map<String, List<String>> headers) {
        if (headers.get(CONTENT_TYPE) != null && !headers.get(CONTENT_TYPE).isEmpty()) {
            return headers.get(CONTENT_TYPE).get(0);
        }
        return null;
    }

    private static JCurl.HttpResponseModel executeWithRetry(
            CloseableHttpClient client, HttpRequestBase request, JCurl.HttpRequestModel requestModel)
            throws IOException {
        int maxRetries = requestModel.getConfig().getMaxRetries();
        int retryDelay = requestModel.getConfig().getRetryDelay();
        int attempts = 0;
        IOException lastException = null;

        while (attempts <= maxRetries) {
            try {
                HttpResponse response = client.execute(request);
                return buildResponse(response, requestModel);
            } catch (IOException e) {
                lastException = e;
                attempts++;

                if (attempts <= maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("retry interrupt", ie);
                    }
                }
            }
        }

        throw new IOException("request failed, retry " + maxRetries + " times", lastException);
    }

    private static JCurl.HttpResponseModel buildResponse(HttpResponse response, JCurl.HttpRequestModel requestModel) throws IOException {
        JCurl.HttpResponseModel result = new JCurl.HttpResponseModel();
        result.setStatusCode(response.getStatusLine().getStatusCode());
        result.setStatusMessage(response.getStatusLine().getReasonPhrase());

        // 响应头
        for (Header header : response.getAllHeaders()) {
            result.addHeader(header.getName(), header.getValue());
        }

        // 响应体
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream inputStream = entity.getContent();
            // 处理压缩
            Header encodingHeader = response.getFirstHeader(CONTENT_ENCODING);
            if (encodingHeader != null) {
                String encoding = encodingHeader.getValue();
                if (GZIP_VALUE.equalsIgnoreCase(encoding)) {
                    inputStream = new GZIPInputStream(inputStream);
                } else if (DEFLATE_VALUE.equalsIgnoreCase(encoding)) {
                    inputStream = new InflaterInputStream(inputStream);
                }
            }
            // 读取响应体（考虑最大下载大小限制）
            byte[] bodyBytes = JCurl.readInputStream(inputStream, requestModel.getConfig().getMaxDownloadSize());
            result.setBodyBytes(bodyBytes);
        }

        return result;
    }
}
