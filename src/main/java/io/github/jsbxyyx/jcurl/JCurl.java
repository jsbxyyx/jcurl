package io.github.jsbxyyx.jcurl;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class JCurl {

    public static class Constants {
        public static final String ACCEPT_ENCODING = "Accept-Encoding";
        public static final String REFERER = "Referer";
        public static final String USER_AGENT = "User-Agent";
        public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
        public static final String AUTHORIZATION = "Authorization";
        public static final String COOKIE = "Cookie";
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String CONTENT_DISPOSITION = "Content-Disposition";
        public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
        public static final String CONTENT_ENCODING = "Content-Encoding";

        public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";
        public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";
        public static final String APPLICATION_X_WWW_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";
        public static final String GZIP_DEFLATE_VALUE = "gzip, deflate";
        public static final String BINARY_VALUE = "binary";
        public static final String GZIP_VALUE = "gzip";
        public static final String DEFLATE_VALUE = "deflate";
        public static final String FORM_DATA_VALUE = "form-data";

        public static final String BASIC_SPACE = "Basic ";
        public static final String BEARER_SPACE = "Bearer ";
        public static final String COLON_SPACE = ": ";
    }

    private final HttpRequestModel request;

    private JCurl() {
        this.request = new HttpRequestModel();
    }

    /**
     * 创建构建器实例
     */
    public static JCurl create() {
        return new JCurl();
    }

    /**
     * 从curl命令创建
     */
    public static JCurl fromCurl(String curlCommand) {
        HttpRequestModel model = parse(curlCommand);
        return fromModel(model);
    }

    /**
     * 从curl命令参数数组创建
     */
    public static JCurl fromCurl(String[] args) {
        HttpRequestModel model = parse(args);
        return fromModel(model);
    }

    /**
     * 从已有模型创建
     */
    public static JCurl fromModel(HttpRequestModel model) {
        if (model == null) {
            throw new IllegalArgumentException("model cannot be null");
        }
        JCurl builder = new JCurl();
        builder.request.setUrl(model.getUrl());
        builder.request.setMethod(model.getMethod());
        Map<String, List<String>> headersCopy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : model.getHeaders().entrySet()) {
            headersCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        builder.request.setHeaders(headersCopy);
        builder.request.setBody(model.getBody());
        builder.request.setBinaryBody(model.getBinaryBody());
        if (model.getFormFields() != null) {
            Map<String, List<HttpRequestModel.FormField>> formFieldsCopy = new LinkedHashMap<>();
            for (Map.Entry<String, List<HttpRequestModel.FormField>> entry : model.getFormFields().entrySet()) {
                formFieldsCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            builder.request.setFormFields(formFieldsCopy);
        }
        Map<String, List<String>> queryParamsCopy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : model.getQueryParams().entrySet()) {
            queryParamsCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        builder.request.setQueryParams(queryParamsCopy);
        builder.request.setUsername(model.getUsername());
        builder.request.setPassword(model.getPassword());
        builder.request.setCookies(new LinkedHashMap<>(model.getCookies()));
        builder.request.setConfig(model.getConfig().copy());
        return builder;
    }

    /**
     * 设置URL
     */
    public JCurl url(String url) {
        request.setUrl(url);
        return this;
    }

    /**
     * 设置请求方法
     */
    public JCurl method(String method) {
        request.setMethod(method);
        return this;
    }

    /**
     * GET请求
     */
    public JCurl get() {
        request.setMethod("GET");
        return this;
    }

    /**
     * POST请求
     */
    public JCurl post() {
        request.setMethod("POST");
        return this;
    }

    /**
     * PUT请求
     */
    public JCurl put() {
        request.setMethod("PUT");
        return this;
    }

    /**
     * DELETE请求
     */
    public JCurl delete() {
        request.setMethod("DELETE");
        return this;
    }

    /**
     * PATCH请求
     */
    public JCurl patch() {
        request.setMethod("PATCH");
        return this;
    }

    /**
     * HEAD请求
     */
    public JCurl head() {
        request.setMethod("HEAD");
        return this;
    }

    /**
     * 添加header（支持重复key）
     */
    public JCurl header(String key, String value) {
        request.addHeader(key, value);
        return this;
    }

    /**
     * 设置header（覆盖已有值）
     */
    public JCurl setHeader(String key, String value) {
        request.setHeader(key, value);
        return this;
    }

    /**
     * 批量添加headers
     */
    public JCurl headers(Map<String, String> headers) {
        headers.forEach((k, v) -> request.addHeader(k, v));
        return this;
    }

    /**
     * 添加查询参数（支持重复key）
     */
    public JCurl queryParam(String key, String value) {
        request.addQueryParam(key, value);
        return this;
    }

    /**
     * 设置查询参数（覆盖已有值）
     */
    public JCurl setQueryParam(String key, String value) {
        request.setQueryParam(key, value);
        return this;
    }

    /**
     * 批量添加查询参数
     */
    public JCurl queryParams(Map<String, String> params) {
        params.forEach((k, v) -> request.addQueryParam(k, v));
        return this;
    }

    /**
     * 设置请求体
     */
    public JCurl body(String body) {
        request.setBody(body);
        return this;
    }

    /**
     * 设置JSON请求体
     */
    public JCurl jsonBody(String json) {
        request.setBody(json);
        request.setHeader("Content-Type", "application/json");
        return this;
    }

    /**
     * 设置二进制请求体
     */
    public JCurl binaryBody(byte[] data) {
        request.setBinaryBody(data);
        return this;
    }

    /**
     * 从文件读取二进制数据
     */
    public JCurl binaryBodyFromFile(String filePath) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(filePath));
        request.setBinaryBody(data);
        return this;
    }

    /**
     * 添加表单字段（文本）
     */
    public JCurl formField(String name, String value) {
        request.addFormField(name, HttpRequestModel.FormField.text(value));
        return this;
    }

    /**
     * 添加表单字段（文件）
     */
    public JCurl formFile(String name, String filePath) {
        request.addFormField(name, HttpRequestModel.FormField.file(filePath));
        return this;
    }

    /**
     * 添加表单字段（文件，指定文件名和类型）
     */
    public JCurl formFile(String name, String filePath, String fileName, String contentType) {
        request.addFormField(
                name, HttpRequestModel.FormField.file(filePath, fileName, contentType));
        return this;
    }

    /**
     * 设置Basic认证
     */
    public JCurl auth(String username, String password) {
        request.setUsername(username);
        request.setPassword(password);
        return this;
    }

    /**
     * 设置Bearer Token
     */
    public JCurl bearerToken(String token) {
        request.setHeader(Constants.AUTHORIZATION, Constants.BEARER_SPACE + token);
        return this;
    }

    /**
     * 添加Cookie
     */
    public JCurl cookie(String name, String value) {
        request.addCookie(name, value);
        return this;
    }

    /**
     * 批量添加Cookie
     */
    public JCurl cookies(Map<String, String> cookies) {
        cookies.forEach((k, v) -> request.addCookie(k, v));
        return this;
    }

    /**
     * 设置User-Agent
     */
    public JCurl userAgent(String userAgent) {
        request.setHeader(Constants.USER_AGENT, userAgent);
        return this;
    }

    /**
     * 设置Referer
     */
    public JCurl referer(String referer) {
        request.setHeader(Constants.REFERER, referer);
        return this;
    }

    /**
     * 启用压缩
     */
    public JCurl compressed() {
        request.getConfig().setCompressed(true);
        request.setHeader(Constants.ACCEPT_ENCODING, Constants.GZIP_DEFLATE_VALUE);
        return this;
    }

    /**
     * 跟随重定向
     */
    public JCurl followRedirects() {
        request.getConfig().setFollowRedirects(true);
        return this;
    }

    /**
     * 跟随重定向（指定是否）
     */
    public JCurl followRedirects(boolean follow) {
        request.getConfig().setFollowRedirects(follow);
        return this;
    }

    /**
     * 允许不安全的SSL连接
     */
    public JCurl insecure() {
        request.getConfig().setVerifySSL(false);
        return this;
    }

    /**
     * SSL验证（指定是否）
     */
    public JCurl verifySSL(boolean verify) {
        request.getConfig().setVerifySSL(verify);
        return this;
    }

    /**
     * 设置连接超时（毫秒）
     */
    public JCurl connectTimeout(int millis) {
        request.getConfig().setConnectTimeout(millis);
        return this;
    }

    /**
     * 设置读取超时（毫秒）
     */
    public JCurl readTimeout(int millis) {
        request.getConfig().setReadTimeout(millis);
        return this;
    }

    /**
     * 设置代理（包含协议URL）
     */
    public JCurl proxy(String proxyUrl) {
        parseAndSetProxy(proxyUrl);
        return this;
    }

    public JCurl proxy(Proxy proxy) {
        request.getConfig().setProxy(proxy);
        return this;
    }

    /**
     * 设置代理认证
     */
    public JCurl proxyAuth(String username, String password) {
        request.getConfig().setProxyUsername(username);
        request.getConfig().setProxyPassword(password);
        return this;
    }

    /**
     * 设置重试次数
     */
    public JCurl retry(int maxRetries) {
        request.getConfig().setMaxRetries(maxRetries);
        return this;
    }

    /**
     * 设置重试延迟（毫秒）
     */
    public JCurl retryDelay(int millis) {
        request.getConfig().setRetryDelay(millis);
        return this;
    }

    /**
     * 设置最大下载大小（字节）
     */
    public JCurl maxDownloadSize(long bytes) {
        request.getConfig().setMaxDownloadSize(bytes);
        return this;
    }

    // ==================== curl选项风格的方法 ====================

    /**
     * curl选项风格的参数设置
     * 例如:  opt("-H", "Content-Type: application/json")
     */
    public JCurl opt(String option, String value) {
        try {
            switch (option) {
                case "-X":
                case "--request":
                    method(value);
                    break;

                case "-H":
                case "--header":
                    parseAndAddHeader(value);
                    break;

                case "-d":
                case "--data":
                case "--data-ascii":
                case "--data-raw":
                    body(value);
                    if (request.getMethod().equals("GET")) {
                        post();
                    }
                    break;

                case "--data-binary":
                    if (value.startsWith("@")) {
                        binaryBodyFromFile(value.substring(1));
                    } else {
                        binaryBody(value.getBytes(StandardCharsets.UTF_8));
                    }
                    if (request.getMethod().equals("GET")) {
                        post();
                    }
                    break;

                case "--data-urlencode":
                    body(urlEncode(value));
                    if (request.getMethod().equals("GET")) {
                        post();
                    }
                    break;

                case "-G":
                case "--get":
                    // 将body转为query参数
                    if (request.getBody() != null) {
                        parseQueryString(request.getBody());
                        request.setBody(null);
                    }
                    get();
                    break;

                case "-F":
                case "--form":
                case "--form-string":
                    parseAndAddFormField(value);
                    if (request.getMethod().equals("GET")) {
                        post();
                    }
                    break;

                case "-u":
                case "--user":
                    parseAndSetAuth(value);
                    break;

                case "-A":
                case "--user-agent":
                    userAgent(value);
                    break;

                case "-e":
                case "--referer":
                    referer(value);
                    break;

                case "-b":
                case "--cookie":
                    parseAndAddCookies(value);
                    break;

                case "-L":
                case "--location":
                    followRedirects();
                    break;

                case "-k":
                case "--insecure":
                    insecure();
                    break;

                case "--compressed":
                    compressed();
                    break;

                case "--connect-timeout":
                    connectTimeout(Integer.parseInt(value) * 1000);
                    break;

                case "-m":
                case "--max-time":
                    readTimeout(Integer.parseInt(value) * 1000);
                    break;

                case "-x":
                case "--proxy":
                    parseAndSetProxy(value);
                    break;

                case "-U":
                case "--proxy-user":
                    parseAndSetProxyAuth(value);
                    break;

                case "--retry":
                    retry(Integer.parseInt(value));
                    break;

                case "--retry-delay":
                    retryDelay(Integer.parseInt(value) * 1000);
                    break;

                case "--x-max-download":
                    maxDownloadSize(Long.parseLong(value));
                    break;

                case "--url":
                    url(value);
                    break;

                default:
                    throw new IllegalArgumentException("unknown: " + option);
            }
        } catch (Exception e) {
            throw new RuntimeException("parse options failed:  " + option + " = " + value, e);
        }
        return this;
    }

    /**
     * curl选项风格的开关参数
     * 例如:  opt("-k") 或 opt("--insecure")
     */
    public JCurl opt(String option) {
        switch (option) {
            case "-G":
            case "--get":
                if (request.getBody() != null) {
                    parseQueryString(request.getBody());
                    request.setBody(null);
                }
                get();
                break;

            case "-L":
            case "--location":
                followRedirects();
                break;

            case "-k":
            case "--insecure":
                insecure();
                break;

            case "--compressed":
                compressed();
                break;

            case "-I":
            case "--head":
                head();
                break;

            default:
                throw new IllegalArgumentException("unknown option: " + option);
        }
        return this;
    }

    // ==================== 辅助方法 ====================

    private void parseAndAddHeader(String header) {
        int colonIndex = header.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException("invalid header format: " + header);
        }
        String key = header.substring(0, colonIndex).trim();
        String value = header.substring(colonIndex + 1).trim();
        header(key, value);
    }

    private void parseAndAddFormField(String formStr) {
        int eqIndex = formStr.indexOf('=');
        if (eqIndex == -1) {
            throw new IllegalArgumentException("invalid form field format:  " + formStr);
        }
        String name = formStr.substring(0, eqIndex).trim();
        String value = formStr.substring(eqIndex + 1).trim();

        if (value.startsWith("@")) {
            formFile(name, value.substring(1));
        } else {
            formField(name, value);
        }
    }

    private void parseAndSetAuth(String auth) {
        int colonIndex = auth.indexOf(':');
        if (colonIndex == -1) {
            auth(auth, "");
        } else {
            auth(auth.substring(0, colonIndex), auth.substring(colonIndex + 1));
        }
    }

    private void parseAndAddCookies(String cookieStr) {
        String cookies = cookieStr;
        if (new File(cookieStr).exists()) {
            try {
                cookies = new String(
                        Files.readAllBytes(Paths.get(cookieStr)), StandardCharsets.UTF_8)
                        .trim();
            } catch (IOException e) {
                // 如果读取失败，当作字符串处理
            }
        }

        String[] pairs = cookies.split(";");
        for (String pair : pairs) {
            String[] parts = pair.trim().split("=", 2);
            if (parts.length == 2) {
                cookie(parts[0].trim(), parts[1].trim());
            }
        }
    }

    // 更新 parseAndSetProxy 方法
    private void parseAndSetProxy(String proxyStr) {
        String temp = proxyStr;
        Proxy.Type proxyType = Proxy.Type.HTTP;

        if (temp.contains("://")) {
            String[] parts = temp.split("://", 2);
            String protocolStr = parts[0].toLowerCase();
            temp = parts[1];

            switch (protocolStr) {
                case "http":
                case "https":
                    proxyType = Proxy.Type.HTTP;
                    break;
                case "socks":
                case "socks4":
                case "socks5":
                    proxyType = Proxy.Type.SOCKS;
                    break;
            }
        }

        request.getConfig().setProxyType(proxyType);

        if (temp.contains(":")) {
            String[] parts = temp.split(":", 2);
            request.getConfig().setProxyHost(parts[0]);
            request.getConfig().setProxyPort(Integer.parseInt(parts[1]));
        } else {
            int defaultPort = (proxyType == Proxy.Type.HTTP) ? 8080 : 1080;
            request.getConfig().setProxyHost(temp);
            request.getConfig().setProxyPort(defaultPort);
        }
    }

    private void parseAndSetProxyAuth(String auth) {
        int colonIndex = auth.indexOf(':');
        if (colonIndex == -1) {
            proxyAuth(auth, "");
        } else {
            proxyAuth(auth.substring(0, colonIndex), auth.substring(colonIndex + 1));
        }
    }

    private void parseQueryString(String queryString) {
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                queryParam(parts[0], parts[1]);
            } else if (parts.length == 1) {
                queryParam(parts[0], "");
            }
        }
    }

    // ==================== 构建和执行 ====================

    /**
     * 构建HttpRequestModel
     */
    public HttpRequestModel build() {
        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            throw new IllegalStateException("URL must be specified");
        }

        // 确保URL有协议
        if (!request.getUrl().startsWith("http://") && !request.getUrl().startsWith("https://")) {
            request.setUrl("https://" + request.getUrl());
        }

        return request;
    }

    /**
     * 获取当前正在构建的请求模型（用于调试）
     */
    public HttpRequestModel peek() {
        return request;
    }

    public static HttpRequestModel parse(String curlCommand) {
        String[] args = tokenize(curlCommand);
        return parse(args);
    }

    public static HttpRequestModel parse(String[] args) {
        HttpRequestModel request = new HttpRequestModel();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("curl")) {
                continue;
            }

            try {
                switch (arg) {
                    case "-X":
                    case "--request":
                        request.setMethod(getNextArg(args, i));
                        i++;
                        break;

                    case "-H":
                    case "--header":
                        parseHeader(request, getNextArg(args, i));
                        i++;
                        break;

                    case "-d":
                    case "--data":
                    case "--data-ascii":
                    case "--data-raw":
                        request.setBody(getNextArg(args, i));
                        if (request.getMethod().equals("GET")) {
                            request.setMethod("POST");
                        }
                        i++;
                        break;

                    case "--data-binary":
                        String binaryData = getNextArg(args, i);
                        if (binaryData.startsWith("@")) {
                            request.setBinaryBody(readBinaryFile(binaryData.substring(1)));
                        } else {
                            request.setBinaryBody(binaryData.getBytes(StandardCharsets.UTF_8));
                        }
                        if (request.getMethod().equals("GET")) {
                            request.setMethod("POST");
                        }
                        i++;
                        break;

                    case "--data-urlencode":
                        String urlencData = getNextArg(args, i);
                        request.setBody(urlEncode(urlencData));
                        if (request.getMethod().equals("GET")) {
                            request.setMethod("POST");
                        }
                        i++;
                        break;

                    case "-G":
                    case "--get":
                        if (request.getBody() != null) {
                            parseQueryString(request, request.getBody());
                            request.setBody(null);
                        }
                        request.setMethod("GET");
                        break;

                    case "-F":
                    case "--form":
                    case "--form-string":
                        parseFormField(request, getNextArg(args, i));
                        if (request.getMethod().equals("GET")) {
                            request.setMethod("POST");
                        }
                        i++;
                        break;

                    case "-u":
                    case "--user":
                        parseAuth(request, getNextArg(args, i));
                        i++;
                        break;

                    case "-A":
                    case "--user-agent":
                        request.addHeader("User-Agent", getNextArg(args, i));
                        i++;
                        break;

                    case "-e":
                    case "--referer":
                        request.addHeader("Referer", getNextArg(args, i));
                        i++;
                        break;

                    case "-b":
                    case "--cookie":
                        parseCookies(request, getNextArg(args, i));
                        i++;
                        break;

                    case "-L":
                    case "--location":
                        request.getConfig().setFollowRedirects(true);
                        break;

                    case "-k":
                    case "--insecure":
                        request.getConfig().setVerifySSL(false);
                        break;

                    case "--compressed":
                        request.getConfig().setCompressed(true);
                        request.addHeader(Constants.ACCEPT_ENCODING, Constants.GZIP_DEFLATE_VALUE);
                        break;

                    case "--connect-timeout":
                        int connectTimeout = Integer.parseInt(getNextArg(args, i));
                        request.getConfig().setConnectTimeout(connectTimeout * 1000);
                        i++;
                        break;

                    case "-m":
                    case "--max-time":
                        int maxTime = Integer.parseInt(getNextArg(args, i));
                        request.getConfig().setReadTimeout(maxTime * 1000);
                        i++;
                        break;

                    case "-x":
                    case "--proxy":
                        parseProxy(request, getNextArg(args, i));
                        i++;
                        break;

                    case "-U":
                    case "--proxy-user":
                        parseProxyAuth(request, getNextArg(args, i));
                        i++;
                        break;

                    case "--retry":
                        request.getConfig().setMaxRetries(Integer.parseInt(getNextArg(args, i)));
                        i++;
                        break;

                    case "--retry-delay":
                        int delay = Integer.parseInt(getNextArg(args, i));
                        request.getConfig().setRetryDelay(delay * 1000);
                        i++;
                        break;

                    case "--x-max-download":
                        request.getConfig().setMaxDownloadSize(Long.parseLong(getNextArg(args, i)));
                        i++;
                        break;

                    case "--url":
                        request.setUrl(getNextArg(args, i));
                        i++;
                        break;

                    default:
                        if (!arg.startsWith("-")) {
                            request.setUrl(arg);
                        }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("parse option failed: " + arg, e);
            }
        }

        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            throw new IllegalArgumentException("URL must be specified");
        }

        if (!request.getUrl().startsWith("http://") && !request.getUrl().startsWith("https://")) {
            request.setUrl("https://" + request.getUrl());
        }

        return request;
    }

    private static String[] tokenize(String curlCommand) {
        List<String> tokens = new ArrayList<>();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escapeNext = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < curlCommand.length(); i++) {
            char ch = curlCommand.charAt(i);

            if (escapeNext) {
                current.append(ch);
                escapeNext = false;
                continue;
            }

            if (ch == '\\') {
                escapeNext = true;
                continue;
            }

            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && Character.isWhitespace(ch)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens.toArray(new String[0]);
    }

    private static String getNextArg(String[] args, int currentIndex) {
        if (currentIndex + 1 >= args.length) {
            throw new IllegalArgumentException("argument expected after: " + args[currentIndex]);
        }
        return args[currentIndex + 1];
    }

    private static void parseHeader(HttpRequestModel request, String header) {
        int colonIndex = header.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException("invalid request header format: " + header);
        }
        String key = header.substring(0, colonIndex).trim();
        String value = header.substring(colonIndex + 1).trim();
        request.addHeader(key, value); // 使用addHeader支持多值
    }

    private static void parseFormField(HttpRequestModel request, String formStr) {
        int eqIndex = formStr.indexOf('=');
        if (eqIndex == -1) {
            throw new IllegalArgumentException("invalid form field format: " + formStr);
        }

        String name = formStr.substring(0, eqIndex).trim();
        String value = formStr.substring(eqIndex + 1).trim();

        HttpRequestModel.FormField field;
        if (value.startsWith("@")) {
            field = HttpRequestModel.FormField.file(value.substring(1));
        } else {
            field = HttpRequestModel.FormField.text(value);
        }

        request.addFormField(name, field);
    }

    private static void parseAuth(HttpRequestModel request, String auth) {
        int colonIndex = auth.indexOf(':');
        if (colonIndex == -1) {
            request.setUsername(auth);
        } else {
            request.setUsername(auth.substring(0, colonIndex));
            request.setPassword(auth.substring(colonIndex + 1));
        }
    }

    private static void parseCookies(HttpRequestModel request, String cookieStr) {
        String cookies = cookieStr;
        if (new File(cookieStr).exists()) {
            try {
                cookies = new String(
                        Files.readAllBytes(Paths.get(cookieStr)), StandardCharsets.UTF_8)
                        .trim();
            } catch (IOException e) {
                // 如果读取失败，当作字符串处理
            }
        }

        String[] pairs = cookies.split(";");
        for (String pair : pairs) {
            String[] parts = pair.trim().split("=", 2);
            if (parts.length == 2) {
                request.addCookie(parts[0].trim(), parts[1].trim());
            }
        }
    }

    private static void parseProxy(HttpRequestModel request, String proxyStr) {
        String temp = proxyStr;
        Proxy.Type proxyType = Proxy.Type.HTTP; // 默认 HTTP

        // 解析协议部分
        if (temp.contains("://")) {
            String[] parts = temp.split("://", 2);
            String protocolStr = parts[0].toLowerCase();
            temp = parts[1];

            proxyType = parseProxyProtocol(protocolStr);
        }

        request.getConfig().setProxyType(proxyType);

        // 解析host和port
        if (temp.contains(":")) {
            String[] parts = temp.split(":", 2);
            request.getConfig().setProxyHost(parts[0]);
            request.getConfig().setProxyPort(Integer.parseInt(parts[1]));
        } else {
            request.getConfig().setProxyHost(temp);
            // 根据代理类型设置默认端口
            int defaultPort = (proxyType == Proxy.Type.HTTP) ? 8080 : 1080;
            request.getConfig().setProxyPort(defaultPort);
        }
    }

    private static Proxy.Type parseProxyProtocol(String protocol) {
        switch (protocol.toLowerCase()) {
            case "http":
            case "https":
                return Proxy.Type.HTTP;

            case "socks":
            case "socks4":
            case "socks5":
                return Proxy.Type.SOCKS;

            default:
                System.err.println("warning: unknown proxy protocol '" + protocol + "', use proxy : HTTP");
                return Proxy.Type.HTTP;
        }
    }

    private static void parseProxyAuth(HttpRequestModel request, String auth) {
        int colonIndex = auth.indexOf(':');
        if (colonIndex == -1) {
            request.getConfig().setProxyUsername(auth);
        } else {
            request.getConfig().setProxyUsername(auth.substring(0, colonIndex));
            request.getConfig().setProxyPassword(auth.substring(colonIndex + 1));
        }
    }

    private static void parseQueryString(HttpRequestModel request, String queryString) {
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                request.addQueryParam(parts[0], parts[1]); // 使用addQueryParam支持多值
            } else if (parts.length == 1) {
                request.addQueryParam(parts[0], "");
            }
        }
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] readBinaryFile(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }

    /**
     * HTTP请求模型 - 支持多值headers和queryParams
     */
    public static class HttpRequestModel {
        private String url;
        private String method = "GET";
        private Map<String, List<String>> headers = new CaseInsensitiveMap<>();
        private String body;
        private byte[] binaryBody;
        private Map<String, List<FormField>> formFields;
        private Map<String, List<String>> queryParams = new LinkedHashMap<>();

        // 认证信息
        private String username;
        private String password;

        // Cookie
        private Map<String, String> cookies = new LinkedHashMap<>();

        // 配置选项
        private RequestConfig config = new RequestConfig();

        // Getters and Setters
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method.toUpperCase();
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, List<String>> headers) {
            this.headers = new CaseInsensitiveMap<>(headers);
        }

        /**
         * 添加单个header值
         */
        public void addHeader(String key, String value) {
            this.headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        /**
         * 设置header（覆盖已有值）
         */
        public void setHeader(String key, String value) {
            List<String> values = new ArrayList<>();
            values.add(value);
            this.headers.put(key, values);
        }

        /**
         * 获取header的第一个值
         */
        public String getHeader(String key) {
            List<String> values = headers.get(key);
            return values != null && !values.isEmpty() ? values.get(0) : null;
        }

        /**
         * 获取header的所有值
         */
        public List<String> getHeaderValues(String key) {
            return headers.getOrDefault(key, Collections.emptyList());
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public byte[] getBinaryBody() {
            return binaryBody;
        }

        public void setBinaryBody(byte[] binaryBody) {
            this.binaryBody = binaryBody;
        }

        public Map<String, List<FormField>> getFormFields() {
            return formFields;
        }

        public void setFormFields(Map<String, List<FormField>> formFields) {
            this.formFields = formFields;
        }

        public void addFormField(String name, FormField field) {
            if (this.formFields == null) this.formFields = new LinkedHashMap<>();
            this.formFields.computeIfAbsent(name, k -> new ArrayList<>()).add(field);
        }

        public void setFormField(String name, FormField field) {
            List<FormField> fields = new ArrayList<>();
            fields.add(field);
            this.formFields.put(name, fields);
        }

        public FormField getFormField(String name) {
            List<FormField> fields = formFields.get(name);
            return fields != null && !fields.isEmpty() ? fields.get(0) : null;
        }

        public List<FormField> getFormFieldValues(String name) {
            return formFields.getOrDefault(name, Collections.emptyList());
        }

        public List<Map.Entry<String, FormField>> getAllFormFields() {
            List<Map.Entry<String, FormField>> result = new ArrayList<>();
            for (Map.Entry<String, List<FormField>> entry : formFields.entrySet()) {
                for (FormField field : entry.getValue()) {
                    result.add(new AbstractMap.SimpleEntry<>(entry.getKey(), field));
                }
            }
            return result;
        }

        public Map<String, List<String>> getQueryParams() {
            return queryParams;
        }

        public void setQueryParams(Map<String, List<String>> queryParams) {
            this.queryParams = queryParams;
        }

        /**
         * 添加单个查询参数值
         */
        public void addQueryParam(String key, String value) {
            this.queryParams.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        /**
         * 设置查询参数（覆盖已有值）
         */
        public void setQueryParam(String key, String value) {
            List<String> values = new ArrayList<>();
            values.add(value);
            this.queryParams.put(key, values);
        }

        /**
         * 获取查询参数的第一个值
         */
        public String getQueryParam(String key) {
            List<String> values = queryParams.get(key);
            return values != null && !values.isEmpty() ? values.get(0) : null;
        }

        /**
         * 获取查询参数的所有值
         */
        public List<String> getQueryParamValues(String key) {
            return queryParams.getOrDefault(key, Collections.emptyList());
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Map<String, String> getCookies() {
            return cookies;
        }

        public void setCookies(Map<String, String> cookies) {
            this.cookies = cookies;
        }

        public void addCookie(String name, String value) {
            this.cookies.put(name, value);
        }

        public RequestConfig getConfig() {
            return config;
        }

        public void setConfig(RequestConfig config) {
            this.config = config;
        }

        /**
         * 获取完整URL（包含查询参数）
         */
        public String getFullUrl() {
            if (queryParams.isEmpty()) {
                return url;
            }

            StringBuilder fullUrl = new StringBuilder(url);
            char separator = url.contains("?") ? '&' : '?';

            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                for (String value : entry.getValue()) {
                    fullUrl.append(separator)
                            .append(urlEncode(entry.getKey()))
                            .append('=')
                            .append(urlEncode(value));
                    separator = '&';
                }
            }

            return fullUrl.toString();
        }

        private String urlEncode(String value) {
            try {
                return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "HttpRequestModel{" + "url='"
                    + url + '\'' + ", method='"
                    + method + '\'' + ", headers="
                    + headers.size() + ", hasBody="
                    + (body != null || binaryBody != null) + ", formFields="
                    + (formFields != null ? formFields.size() : 0) + ", queryParams="
                    + queryParams.size() + ", config="
                    + config + '}';
        }

        /**
         * 表单字段
         */
        public static class FormField {
            private String value;
            private String filePath;
            private String fileName;
            private String contentType;
            private boolean isFile;

            public static FormField text(String value) {
                FormField field = new FormField();
                field.value = value;
                field.isFile = false;
                return field;
            }

            public static FormField file(String filePath) {
                FormField field = new FormField();
                field.filePath = filePath;
                field.isFile = true;
                field.fileName = Paths.get(filePath).getFileName().toString();
                return field;
            }

            public static FormField file(String filePath, String fileName, String contentType) {
                FormField field = new FormField();
                field.filePath = filePath;
                field.fileName = fileName;
                field.contentType = contentType;
                field.isFile = true;
                return field;
            }

            // Getters and Setters
            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

            public String getFilePath() {
                return filePath;
            }

            public void setFilePath(String filePath) {
                this.filePath = filePath;
            }

            public String getFileName() {
                return fileName;
            }

            public void setFileName(String fileName) {
                this.fileName = fileName;
            }

            public String getContentType() {
                return contentType;
            }

            public void setContentType(String contentType) {
                this.contentType = contentType;
            }

            public boolean isFile() {
                return isFile;
            }

            public void setFile(boolean file) {
                isFile = file;
            }
        }

        /**
         * 请求配置
         */
        public static class RequestConfig {
            private int connectTimeout = 30000;
            private int readTimeout = 60000;
            private boolean followRedirects = false;
            private boolean verifySSL = true;
            private boolean compressed = false;
            private Proxy proxy;
            private Proxy.Type proxyType = Proxy.Type.HTTP;
            private String proxyHost;
            private int proxyPort;
            private String proxyUsername;
            private String proxyPassword;
            private int maxRetries = 0;
            private int retryDelay = 1000;
            private long maxDownloadSize = 0;

            // Getters and Setters
            public int getConnectTimeout() {
                return connectTimeout;
            }

            public void setConnectTimeout(int connectTimeout) {
                this.connectTimeout = connectTimeout;
            }

            public int getReadTimeout() {
                return readTimeout;
            }

            public void setReadTimeout(int readTimeout) {
                this.readTimeout = readTimeout;
            }

            public boolean isFollowRedirects() {
                return followRedirects;
            }

            public void setFollowRedirects(boolean followRedirects) {
                this.followRedirects = followRedirects;
            }

            public boolean isVerifySSL() {
                return verifySSL;
            }

            public void setVerifySSL(boolean verifySSL) {
                this.verifySSL = verifySSL;
            }

            public boolean isCompressed() {
                return compressed;
            }

            public void setCompressed(boolean compressed) {
                this.compressed = compressed;
            }

            public void setProxy(Proxy proxy) {
                this.proxy = proxy;
            }

            public Proxy getProxy() {
                return proxy;
            }

            public Proxy.Type getProxyType() {
                return proxyType;
            }

            public void setProxyType(Proxy.Type proxyType) {
                this.proxyType = proxyType;
            }

            public boolean isSocksProxy() {
                return proxyType == Proxy.Type.SOCKS
                        || (proxy != null && proxy.type() == Proxy.Type.SOCKS);
            }

            public String getProxyHost() {
                return proxyHost;
            }

            public void setProxyHost(String proxyHost) {
                this.proxyHost = proxyHost;
            }

            public int getProxyPort() {
                return proxyPort;
            }

            public void setProxyPort(int proxyPort) {
                this.proxyPort = proxyPort;
            }

            public String getProxyUsername() {
                return proxyUsername;
            }

            public void setProxyUsername(String proxyUsername) {
                this.proxyUsername = proxyUsername;
            }

            public String getProxyPassword() {
                return proxyPassword;
            }

            public void setProxyPassword(String proxyPassword) {
                this.proxyPassword = proxyPassword;
            }

            public int getMaxRetries() {
                return maxRetries;
            }

            public void setMaxRetries(int maxRetries) {
                this.maxRetries = maxRetries;
            }

            public int getRetryDelay() {
                return retryDelay;
            }

            public void setRetryDelay(int retryDelay) {
                this.retryDelay = retryDelay;
            }

            public long getMaxDownloadSize() {
                return maxDownloadSize;
            }

            public void setMaxDownloadSize(long maxDownloadSize) {
                this.maxDownloadSize = maxDownloadSize;
            }

            public RequestConfig copy() {
                HttpRequestModel.RequestConfig config = new HttpRequestModel.RequestConfig();
                config.setConnectTimeout(getConnectTimeout());
                config.setReadTimeout(getReadTimeout());
                config.setFollowRedirects(isFollowRedirects());
                config.setVerifySSL(isVerifySSL());
                config.setCompressed(isCompressed());
                config.setProxy(getProxy());
                config.setProxyType(getProxyType());
                config.setProxyHost(getProxyHost());
                config.setProxyPort(getProxyPort());
                config.setProxyUsername(getProxyUsername());
                config.setProxyPassword(getProxyPassword());
                config.setMaxRetries(getMaxRetries());
                config.setRetryDelay(getRetryDelay());
                config.setMaxDownloadSize(getMaxDownloadSize());
                return config;
            }
        }
    }

    /**
     * HTTP响应模型
     */
    public static class HttpResponseModel {
        private int statusCode;
        private String statusMessage;
        private Map<String, List<String>> headers = new CaseInsensitiveMap<>();
        private byte[] bodyBytes;

        // Getters and Setters
        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public void setStatusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        public void addHeader(String name, String value) {
            this.headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        }

        public void setHeader(String name, String value) {
            List<String> values = new ArrayList<>();
            values.add(value);
            this.headers.put(name, values);
        }

        public String getHeader(String name) {
            List<String> values = headers.get(name);
            return values != null && !values.isEmpty() ? values.get(0) : null;
        }

        public List<String> getHeaderValues(String name) {
            return headers.getOrDefault(name, Collections.emptyList());
        }

        public String getBody() {
            return getBody(StandardCharsets.UTF_8);
        }

        public String getBody(Charset charset) {
            if (bodyBytes == null) {
                return null;
            }
            return new String(bodyBytes, charset);
        }

        public byte[] getBodyBytes() {
            return bodyBytes;
        }

        public void setBodyBytes(byte[] bodyBytes) {
            this.bodyBytes = bodyBytes;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }

        @Override
        public String toString() {
            return "HttpResponseModel{" + "statusCode="
                    + statusCode + ", statusMessage='"
                    + statusMessage + '\'' + ", headers="
                    + headers.size() + ", bodyLength="
                    + (bodyBytes != null ? bodyBytes.length : 0) + '}';
        }
    }

    /**
     * 忽略大小写的Map实现
     *
     * @param <V>
     */
    private static class CaseInsensitiveMap<V> implements Map<String, V> {

        private Map<String, V> wrappedMap;

        public CaseInsensitiveMap() {
            this(new LinkedHashMap<String, V>());
        }

        public CaseInsensitiveMap(Map<String, V> wrappedMap) {
            this.wrappedMap = wrappedMap;
        }

        public int size() {
            return wrappedMap.size();
        }

        public boolean isEmpty() {
            return wrappedMap.isEmpty();
        }

        public boolean containsKey(Object key) {
            if (key == null || !(key instanceof String)) {
                return false;
            }
            return wrappedMap.containsKey(key.toString().toLowerCase());
        }

        public boolean containsValue(Object value) {
            return wrappedMap.containsValue(value);
        }

        public V get(Object key) {
            if (key == null || !(key instanceof String)) {
                return null;
            }
            return wrappedMap.get(key.toString().toLowerCase());
        }

        public V put(String key, V value) {
            if (key == null) {
                throw new NullPointerException("key cannot be null");
            }
            return wrappedMap.put(key.toLowerCase(), value);
        }

        public V remove(Object key) {
            if (key == null || !(key instanceof String)) {
                return null;
            }
            return wrappedMap.remove(key.toString().toLowerCase());
        }

        public void putAll(Map<? extends String, ? extends V> m) {
            if (m == null) {
                throw new NullPointerException("map cannot be null");
            }
            for (java.util.Map.Entry<? extends String, ? extends V> entry : m.entrySet()) {
                this.put(entry.getKey(), entry.getValue());
            }
        }

        public void clear() {
            wrappedMap.clear();
        }

        public Set<String> keySet() {
            return wrappedMap.keySet();
        }

        public Collection<V> values() {
            return wrappedMap.values();
        }

        public Set<java.util.Map.Entry<String, V>> entrySet() {
            return wrappedMap.entrySet();
        }

    }

    public HttpResponseModel exec() throws IOException {
        return exec(HttpUrlConnectionExecutor.create());
    }

    public HttpResponseModel exec(HttpExecutor executor) throws IOException {
        return executor.execute(request);
    }

    interface HttpExecutor {
        JCurl.HttpResponseModel execute(JCurl.HttpRequestModel requestModel) throws IOException;
    }

    public static class HttpUrlConnectionExecutor implements JCurl.HttpExecutor {

        private static final String CRLF = "\r\n";
        private static final String BOUNDARY_PREFIX = "----JCurlFormBoundary";

        static {
            try {
                // Try to enable the setting to restricted headers like "Origin", this is expected to be executed before HttpURLConnection class-loading
                System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
            } catch (Exception ignored) {
            }
        }

        private static final HttpUrlConnectionExecutor executor = new HttpUrlConnectionExecutor();

        private HttpUrlConnectionExecutor() {
        }

        public static HttpUrlConnectionExecutor create() {
            return executor;
        }

        @Override
        public JCurl.HttpResponseModel execute(JCurl.HttpRequestModel requestModel) throws IOException {
            int maxRetries = requestModel.getConfig().getMaxRetries();
            int retryDelay = requestModel.getConfig().getRetryDelay();

            IOException lastException = null;

            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    return doExecute(requestModel);
                } catch (IOException e) {
                    lastException = e;
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("request interrupt", ie);
                        }
                    }
                }
            }

            throw lastException;
        }

        private JCurl.HttpResponseModel doExecute(JCurl.HttpRequestModel requestModel) throws IOException {
            HttpURLConnection connection = null;

            try {
                connection = createConnection(requestModel);
                configureConnection(connection, requestModel);
                setHeaders(connection, requestModel);
                sendRequestBody(connection, requestModel);
                return getResponse(connection, requestModel);

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        private HttpURLConnection createConnection(JCurl.HttpRequestModel requestModel) throws IOException {
            URL url = new URL(requestModel.getFullUrl());

            JCurl.HttpRequestModel.RequestConfig config = requestModel.getConfig();
            Proxy proxy = createProxy(config);

            HttpURLConnection connection;
            if (proxy != null) {
                connection = (HttpURLConnection) url.openConnection(proxy);
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            // 配置 SSL
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                if (!config.isVerifySSL()) {
                    installTrustAllCerts(httpsConnection);
                }
            }

            return connection;
        }

        private Proxy createProxy(JCurl.HttpRequestModel.RequestConfig config) {
            if (config.getProxy() != null) {
                return config.getProxy();
            } else {
                if (config.getProxyHost() == null || config.getProxyHost().trim().isEmpty()) {
                    return null;
                }

                InetSocketAddress proxyAddr = new InetSocketAddress(
                        config.getProxyHost(),
                        config.getProxyPort()
                );

                return new Proxy(config.getProxyType(), proxyAddr);
            }
        }

        private void configureConnection(HttpURLConnection connection, JCurl.HttpRequestModel requestModel) throws IOException {
            JCurl.HttpRequestModel.RequestConfig config = requestModel.getConfig();

            // 设置请求方法
            connection.setRequestMethod(requestModel.getMethod());

            // 设置超时
            connection.setConnectTimeout(config.getConnectTimeout());
            connection.setReadTimeout(config.getReadTimeout());

            // 设置重定向
            connection.setInstanceFollowRedirects(config.isFollowRedirects());

            // 如果有请求体，需要设置 doOutput
            boolean hasBody = requestModel.getBody() != null
                    || requestModel.getBinaryBody() != null
                    || requestModel.getFormFields() != null;
            connection.setDoOutput(hasBody);
            connection.setDoInput(true);

            // 设置代理认证
            if (config.getProxyUsername() != null && !config.getProxyUsername().isEmpty()) {
                String proxyAuth = config.getProxyUsername() + ":" +
                        (config.getProxyPassword() != null ? config.getProxyPassword() : "");
                String encodedProxyAuth = Base64.getEncoder()
                        .encodeToString(proxyAuth.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty(Constants.PROXY_AUTHORIZATION, Constants.BASIC_SPACE + encodedProxyAuth);
            }
        }

        private void setHeaders(HttpURLConnection connection, JCurl.HttpRequestModel requestModel) {
            // 设置自定义 headers（支持多值）
            for (Map.Entry<String, List<String>> entry : requestModel.getHeaders().entrySet()) {
                for (String value : entry.getValue()) {
                    connection.addRequestProperty(entry.getKey(), value);
                }
            }

            // 设置 Basic 认证
            if (requestModel.getUsername() != null) {
                String auth = requestModel.getUsername() + ":" +
                        (requestModel.getPassword() != null ? requestModel.getPassword() : "");
                String encodedAuth = Base64.getEncoder()
                        .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty(Constants.AUTHORIZATION, Constants.BASIC_SPACE + encodedAuth);
            }

            // 设置 Cookies
            if (!requestModel.getCookies().isEmpty()) {
                StringBuilder cookieHeader = new StringBuilder();
                for (Map.Entry<String, String> cookie : requestModel.getCookies().entrySet()) {
                    if (cookieHeader.length() > 0) {
                        cookieHeader.append("; ");
                    }
                    cookieHeader.append(cookie.getKey()).append("=").append(cookie.getValue());
                }
                connection.setRequestProperty(Constants.COOKIE, cookieHeader.toString());
            }
        }

        private void sendRequestBody(HttpURLConnection connection, JCurl.HttpRequestModel requestModel)
                throws IOException {

            // 处理表单数据
            if (requestModel.getFormFields() != null && !requestModel.getFormFields().isEmpty()) {
                sendMultipartFormData(connection, requestModel);
                return;
            }

            // 处理二进制数据
            if (requestModel.getBinaryBody() != null) {
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(requestModel.getBinaryBody());
                    os.flush();
                }
                return;
            }

            // 处理普通文本数据
            if (requestModel.getBody() != null) {
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(requestModel.getBody().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
                return;
            }
        }

        private void sendMultipartFormData(HttpURLConnection connection, JCurl.HttpRequestModel requestModel)
                throws IOException {
            String boundary = BOUNDARY_PREFIX + System.currentTimeMillis();
            connection.setRequestProperty(Constants.CONTENT_TYPE, Constants.MULTIPART_FORM_DATA_VALUE + "; boundary=" + boundary);

            try (OutputStream os = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

                for (Map.Entry<String, JCurl.HttpRequestModel.FormField> entry : requestModel.getAllFormFields()) {
                    String fieldName = entry.getKey();
                    JCurl.HttpRequestModel.FormField field = entry.getValue();

                    if (field.isFile()) {
                        writeFilePart(writer, os, boundary, fieldName, field);
                    } else {
                        writeTextPart(writer, boundary, fieldName, field.getValue());
                    }
                }

                // 结束边界
                writer.append("--").append(boundary).append("--").append(CRLF);
                writer.flush();
            }
        }

        private void writeTextPart(PrintWriter writer, String boundary, String fieldName, String value) {
            writer.append("--").append(boundary).append(CRLF);
            writer.append(Constants.CONTENT_DISPOSITION).append(Constants.COLON_SPACE).append(Constants.FORM_DATA_VALUE)
                    .append("; name=\"").append(fieldName).append("\"").append(CRLF);
            writer.append(CRLF);
            writer.append(value).append(CRLF);
            writer.flush();
        }

        private void writeFilePart(PrintWriter writer, OutputStream os, String boundary,
                                   String fieldName, JCurl.HttpRequestModel.FormField field) throws IOException {
            writer.append("--").append(boundary).append(CRLF);
            writer.append(Constants.CONTENT_DISPOSITION).append(Constants.COLON_SPACE).append(Constants.FORM_DATA_VALUE)
                    .append("; name=\"").append(fieldName)
                    .append("\"; filename=\"").append(field.getFileName()).append("\"")
                    .append(CRLF);

            String contentType = field.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = URLConnection.guessContentTypeFromName(field.getFileName());
                if (contentType == null) {
                    contentType = Constants.APPLICATION_OCTET_STREAM_VALUE;
                }
            }
            writer.append(Constants.CONTENT_TYPE).append(Constants.COLON_SPACE).append(contentType).append(CRLF);
            writer.append(Constants.CONTENT_TRANSFER_ENCODING).append(Constants.COLON_SPACE).append(Constants.BINARY_VALUE).append(CRLF);
            writer.append(CRLF);
            writer.flush();

            // 写入文件内容
            try (InputStream inputStream = Files.newInputStream(Paths.get(field.getFilePath()))) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            os.write(CRLF.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        private JCurl.HttpResponseModel getResponse(HttpURLConnection connection,
                                                    JCurl.HttpRequestModel requestModel) throws IOException {
            JCurl.HttpResponseModel response = new JCurl.HttpResponseModel();

            // 获取状态码
            response.setStatusCode(connection.getResponseCode());
            response.setStatusMessage(connection.getResponseMessage());

            // 获取响应头
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    // 添加所有值
                    for (String value : entry.getValue()) {
                        response.addHeader(entry.getKey(), value);
                    }
                }
            }

            // 读取响应体
            InputStream inputStream = null;
            try {
                if (response.getStatusCode() >= 400) {
                    inputStream = connection.getErrorStream();
                } else {
                    inputStream = connection.getInputStream();
                }
                if (inputStream != null) {
                    // 处理压缩
                    String contentEncoding = connection.getContentEncoding();
                    if (Constants.GZIP_VALUE.equalsIgnoreCase(contentEncoding)) {
                        inputStream = new GZIPInputStream(inputStream);
                    }
                    // 读取响应体（考虑最大下载大小限制）
                    byte[] bodyBytes = readInputStream(inputStream, requestModel.getConfig().getMaxDownloadSize());
                    response.setBodyBytes(bodyBytes);
                }
            } catch (IOException e) {
                // 如果读取响应体失败，不抛出异常，只是没有响应体
                response.setBodyBytes(new byte[0]);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            return response;
        }

        private byte[] readInputStream(InputStream inputStream, long maxDownloadSize) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = inputStream.read(data)) != -1) {
                totalBytesRead += bytesRead;

                // 检查最大下载大小限制
                if (maxDownloadSize > 0 && totalBytesRead > maxDownloadSize) {
                    throw new IOException("response body size more than max-download-size limit:  " + maxDownloadSize + " bytes");
                }

                buffer.write(data, 0, bytesRead);
            }

            return buffer.toByteArray();
        }

        private void installTrustAllCerts(HttpsURLConnection connection) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            @Override
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                connection.setSSLSocketFactory(sslContext.getSocketFactory());
                connection.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException("config SSL failed.", e);
            }
        }
    }
}
