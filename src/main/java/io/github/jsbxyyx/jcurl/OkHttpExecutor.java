package io.github.jsbxyyx.jcurl;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static io.github.jsbxyyx.jcurl.JCurl.Constants.APPLICATION_OCTET_STREAM_VALUE;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.APPLICATION_X_WWW_FORM_URLENCODED_VALUE;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.AUTHORIZATION;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.CONTENT_ENCODING;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.CONTENT_TYPE;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.COOKIE;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.DEFLATE_VALUE;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.GZIP_VALUE;
import static io.github.jsbxyyx.jcurl.JCurl.Constants.PROXY_AUTHORIZATION;

/**
 * OkHttp实现的HTTP请求执行器
 * 需要依赖: com.squareup.okhttp3:okhttp:4.x
 */
public class OkHttpExecutor implements JCurl.HttpExecutor {

    private static final OkHttpExecutor executor = new OkHttpExecutor();

    private OkHttpExecutor() {
    }

    public static OkHttpExecutor create() {
        return executor;
    }

    public JCurl.HttpResponseModel execute(JCurl.HttpRequestModel requestModel) throws IOException {
        OkHttpClient client = buildClient(requestModel);
        Request request = buildRequest(requestModel);

        // 执行请求（带重试）
        return executeWithRetry(
                client,
                request,
                requestModel.getConfig().getMaxRetries(),
                requestModel.getConfig().getRetryDelay());
    }

    private static OkHttpClient buildClient(JCurl.HttpRequestModel requestModel) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(requestModel.getConfig().getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(requestModel.getConfig().getReadTimeout(), TimeUnit.MILLISECONDS)
                .followRedirects(requestModel.getConfig().isFollowRedirects())
                .followSslRedirects(requestModel.getConfig().isFollowRedirects());

        // SSL验证
        if (!requestModel.getConfig().isVerifySSL()) {
            trustAllCertificates(builder);
        }

        // 代理设置
        boolean isProxy = false;
        if (requestModel.getConfig().getProxy() == null) {
            if (requestModel.getConfig().getProxyHost() != null
                    && requestModel.getConfig().getProxyPort() > 0) {
                Proxy proxy = new Proxy(
                        requestModel.getConfig().getProxyType(),
                        new InetSocketAddress(
                                requestModel.getConfig().getProxyHost(),
                                requestModel.getConfig().getProxyPort()));
                builder.proxy(requestModel.getConfig().getProxy());
                isProxy = true;
            }
        } else {
            builder.proxy(requestModel.getConfig().getProxy());
            isProxy = true;
        }
        if (isProxy) {
            // HTTP代理认证
            if (requestModel.getConfig().getProxy().type() == Proxy.Type.HTTP
                    && requestModel.getConfig().getProxyUsername() != null) {
                builder.proxyAuthenticator((route, response) -> {
                    String credential = Credentials.basic(
                            requestModel.getConfig().getProxyUsername(),
                            requestModel.getConfig().getProxyPassword() != null
                                    ? requestModel.getConfig().getProxyPassword()
                                    : "");
                    return response.request()
                            .newBuilder()
                            .header(PROXY_AUTHORIZATION, credential)
                            .build();
                });
            }
            // SOCKS代理认证
            if (requestModel.getConfig().isSocksProxy()
                    && requestModel.getConfig().getProxyUsername() != null) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                requestModel.getConfig().getProxyUsername(),
                                requestModel.getConfig().getProxyPassword() == null
                                        ? "".toCharArray()
                                        : requestModel
                                        .getConfig()
                                        .getProxyPassword()
                                        .toCharArray());
                    }
                });
            }
        }

        return builder.build();
    }

    private static Request buildRequest(JCurl.HttpRequestModel requestModel) {
        Request.Builder builder = new Request.Builder().url(requestModel.getFullUrl());

        // 添加请求头（支持多值）
        for (Map.Entry<String, List<String>> header : requestModel.getHeaders().entrySet()) {
            for (String value : header.getValue()) {
                builder.addHeader(header.getKey(), value);
            }
        }

        // 添加Cookie
        if (!requestModel.getCookies().isEmpty()) {
            StringBuilder cookieHeader = new StringBuilder();
            for (Map.Entry<String, String> cookie : requestModel.getCookies().entrySet()) {
                if (cookieHeader.length() > 0) cookieHeader.append("; ");
                cookieHeader.append(cookie.getKey()).append("=").append(cookie.getValue());
            }
            builder.addHeader(COOKIE, cookieHeader.toString());
        }

        // Basic认证
        if (requestModel.getUsername() != null) {
            String credential = Credentials.basic(
                    requestModel.getUsername(),
                    requestModel.getPassword() != null ? requestModel.getPassword() : "");
            builder.addHeader(AUTHORIZATION, credential);
        }

        // 构建请求体
        RequestBody body = buildRequestBody(requestModel);

        // 设置请求方法
        builder.method(requestModel.getMethod(), body);

        return builder.build();
    }

    private static RequestBody buildRequestBody(JCurl.HttpRequestModel requestModel) {
        // 无请求体
        if (requestModel.getBody() == null
                && requestModel.getBinaryBody() == null
                && requestModel.getFormFields() == null) {
            return null;
        }

        // Multipart表单
        if (requestModel.getFormFields() != null) {
            MultipartBody.Builder multipartBuilder =
                    new MultipartBody.Builder().setType(MultipartBody.FORM);

            for (Map.Entry<String, JCurl.HttpRequestModel.FormField> entry : requestModel.getAllFormFields()) {
                JCurl.HttpRequestModel.FormField field = entry.getValue();

                if (field.isFile()) {
                    File file = new File(field.getFilePath());
                    MediaType mediaType = field.getContentType() != null
                            ? MediaType.parse(field.getContentType())
                            : MediaType.parse(APPLICATION_OCTET_STREAM_VALUE);
                    multipartBuilder.addFormDataPart(
                            entry.getKey(),
                            field.getFileName(),
                            RequestBody.create(file, mediaType));
                } else {
                    multipartBuilder.addFormDataPart(entry.getKey(), field.getValue());
                }
            }

            return multipartBuilder.build();
        }

        // 二进制数据
        if (requestModel.getBinaryBody() != null) {
            MediaType mediaType = getMediaType(requestModel.getHeaders());
            return RequestBody.create(requestModel.getBinaryBody(), mediaType);
        }

        // 文本数据
        if (requestModel.getBody() != null) {
            MediaType mediaType = getMediaType(requestModel.getHeaders());
            return RequestBody.create(requestModel.getBody(), mediaType);
        }

        return null;
    }

    private static MediaType getMediaType(Map<String, List<String>> headers) {
        String contentType = null;
        if (headers.get(CONTENT_TYPE) == null && !headers.get(CONTENT_TYPE).isEmpty()) {
            contentType = headers.get(CONTENT_TYPE).get(0);
        }
        return contentType != null
                ? MediaType.parse(contentType)
                : MediaType.parse(APPLICATION_X_WWW_FORM_URLENCODED_VALUE);
    }

    private static JCurl.HttpResponseModel executeWithRetry(
            OkHttpClient client, Request request, int maxRetries, int retryDelay)
            throws IOException {
        int attempts = 0;
        IOException lastException = null;

        while (attempts <= maxRetries) {
            try {
                Response response = client.newCall(request).execute();
                return buildResponse(response);
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

        throw new IOException("request failed，retry " + maxRetries + " times", lastException);
    }

    private static JCurl.HttpResponseModel buildResponse(Response response) throws IOException {
        JCurl.HttpResponseModel result = new JCurl.HttpResponseModel();
        result.setStatusCode(response.code());
        result.setStatusMessage(response.message());

        // 响应头
        for (String name : response.headers().names()) {
            for (String header : response.headers(name)) {
                result.addHeader(name, header);
            }
        }

        // 响应体
        if (response.body() != null) {
            byte[] bodyBytes = response.body().bytes();
            // 处理gzip压缩
            String encoding = response.header(CONTENT_ENCODING);
            if (GZIP_VALUE.equalsIgnoreCase(encoding)) {
                bodyBytes = decompressGzip(bodyBytes);
            } else if (DEFLATE_VALUE.equalsIgnoreCase(encoding)) {
                bodyBytes = decompressDeflate(bodyBytes);
            }
            result.setBodyBytes(bodyBytes);
        }

        return result;
    }

    private static void trustAllCertificates(OkHttpClient.Builder builder) {
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

            builder.sslSocketFactory(
                    sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new RuntimeException("config SSL failed.", e);
        }
    }

    /**
     * 解压gzip数据
     */
    private static byte[] decompressGzip(byte[] compressed) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
             GZIPInputStream gzipStream = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzipStream.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }

    /**
     * 解压deflate数据
     */
    private static byte[] decompressDeflate(byte[] compressed) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
             InflaterInputStream inflaterStream = new InflaterInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = inflaterStream.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }
}
