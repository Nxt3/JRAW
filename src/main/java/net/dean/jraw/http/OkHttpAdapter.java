package net.dean.jraw.http;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import okio.BufferedSink;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class OkHttpAdapter implements HttpAdapter {
    private OkHttpClient http;
    private CookieManager cookieManager;
    private Map<String, String> defaultHeaders;

    public OkHttpAdapter() {
        this.http = new OkHttpClient();
        this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        this.defaultHeaders = new HashMap<>();
        http.setCookieHandler(cookieManager);
    }

    @Override
    public RestResponse execute(HttpRequest request) throws NetworkException, IOException {
        Request.Builder builder = new Request.Builder()
                .method(request.getMethod(), request.getBody() == null ? null : new OkHttpRequestBody(request.getBody()))
                .url(request.getUrl())
                .headers(request.getHeaders());

        Response response = http.newCall(builder.build()).execute();
        if (!response.isSuccessful())
            throw new NetworkException(response.code());

        return new RestResponse(request, response.body().source().inputStream(), response.headers(), response.code(),
                response.message(), response.protocol().toString().toUpperCase());
    }

    @Override
    public int getCode(Object response) {
        return ((Response) response).code();
    }

    @Override
    public int getConnectTimeout() {
        return http.getConnectTimeout();
    }

    @Override
    public void setConnectTimeout(long timeout, TimeUnit unit) {
        http.setConnectTimeout(timeout, unit);
    }

    @Override
    public int getReadTimeout() {
        return http.getReadTimeout();
    }

    @Override
    public void setReadTimeout(long timeout, TimeUnit unit) {
        http.setReadTimeout(timeout, unit);
    }

    @Override
    public int getWriteTimeout() {
        return http.getWriteTimeout();
    }

    @Override
    public void setWriteTimeout(long timeout, TimeUnit unit) {
        http.setWriteTimeout(timeout, unit);
    }

    @Override
    public boolean isFollowingRedirects() {
        return http.getFollowRedirects();
    }

    @Override
    public void setFollowRedirects(boolean flag) {
        http.setFollowRedirects(flag);
    }

    @Override
    public Proxy getProxy() {
        return http.getProxy();
    }

    @Override
    public void setProxy(Proxy proxy) {
        http.setProxy(proxy);
    }

    @Override
    public CookieManager getCookieManager() {
        return cookieManager;
    }

    @Override
    public void setCookieManager(CookieManager manager) {
        this.cookieManager = manager;
        http.setCookieHandler(cookieManager);
    }

    @Override
    public void authenticate(BasicAuthData authData) {
        http.setAuthenticator(new BasicAuthenticator(authData.getUsername(), authData.getPassword()));
    }

    @Override
    public void deauthenticate() {
        http.setAuthenticator(null);
    }

    @Override
    public void setDefaultHeader(String key, String value) {
        defaultHeaders.put(key, value);
    }

    @Override
    public void removeDefaultHeader(String key) {
        defaultHeaders.remove(key);
    }

    @Override
    public String getDefaultHeader(String key) {
        return defaultHeaders.get(key);
    }

    @Override
    public Map<String, String> getDefaultHeaders() {
        return new HashMap<>(defaultHeaders);
    }

    /** Mirrors a JRAW RequestBody to an OkHttp RequestBody */
    private class OkHttpRequestBody extends com.squareup.okhttp.RequestBody {
        private RequestBody mirror;
        private com.squareup.okhttp.MediaType contentType = null; // Lazily initialized

        public OkHttpRequestBody(RequestBody mirror) {
            this.mirror = mirror;
        }

        @Override
        public com.squareup.okhttp.MediaType contentType() {
            if (mirror.contentType() == null)
                return null;
            if (contentType != null)
                return contentType;
            contentType = com.squareup.okhttp.MediaType.parse(mirror.contentType().toString());
            return contentType;
        }

        @Override
        public long contentLength() {
            return mirror.contentLength();
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            mirror.writeTo(sink);
        }
    }
}
