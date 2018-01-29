package com.chopsticks.mvc.http;

import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.chopsticks.exception.NotFoundException;
import com.chopsticks.kit.StringKit;
import com.chopsticks.mvc.WebContext;
import com.chopsticks.mvc.ui.ModelAndView;
import com.chopsticks.mvc.wrapper.OutputStreamWrapper;
import com.chopsticks.server.netty.HttpConst;
import com.chopsticks.server.netty.ProgressiveFutureListener;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * HttpResponse
 *
 * @author biezhi
 * 2017/5/31
 */
@Slf4j
public class HttpResponse implements Response {

    private HttpHeaders           headers     = new DefaultHttpHeaders(false);
    private Set<Cookie>           cookies     = new HashSet<>(4);
    private int                   statusCode  = 200;
    private boolean               isCommit    = false;
    private ChannelHandlerContext ctx         = null;
    private CharSequence          contentType = null;
    private CharSequence          dateString  = null;

    @Override
    public int statusCode() {
        return this.statusCode;
    }

    @Override
    public Response status(int status) {
        this.statusCode = status;
        return this;
    }

    @Override
    public Response contentType(@NonNull CharSequence contentType) {
        this.contentType = contentType;
        return this;
    }

    @Override
    public String contentType() {
        return null == this.contentType ? null : String.valueOf(this.contentType);
    }

    @Override
    public Map<String, String> headers() {
        Map<String, String> map = new HashMap<>(this.headers.size());
        this.headers.forEach(header -> map.put(header.getKey(), header.getValue()));
        return map;
    }

    @Override
    public Response header(CharSequence name, CharSequence value) {
        this.headers.set(name, value);
        return this;
    }

    @Override
    public Response cookie(@NonNull com.chopsticks.mvc.http.Cookie cookie) {
        Cookie nettyCookie = new io.netty.handler.codec.http.cookie.DefaultCookie(cookie.getName(), cookie.getValue());
        if (cookie.getDomain() != null) {
            nettyCookie.setDomain(cookie.getDomain());
        }
        if (cookie.getMaxAge() > 0) {
            nettyCookie.setMaxAge(cookie.getMaxAge());
        }
        nettyCookie.setPath(cookie.getPath());
        nettyCookie.setHttpOnly(cookie.isHttpOnly());
        nettyCookie.setSecure(cookie.isSecure());
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public Response cookie(String name, String value) {
        this.cookies.add(new io.netty.handler.codec.http.cookie.DefaultCookie(name, value));
        return this;
    }

    @Override
    public Response cookie(@NonNull String name, @NonNull String value, int maxAge) {
        Cookie nettyCookie = new io.netty.handler.codec.http.cookie.DefaultCookie(name, value);
        nettyCookie.setPath("/");
        nettyCookie.setMaxAge(maxAge);
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public Response cookie(@NonNull String name, @NonNull String value, int maxAge, boolean secured) {
        Cookie nettyCookie = new io.netty.handler.codec.http.cookie.DefaultCookie(name, value);
        nettyCookie.setPath("/");
        nettyCookie.setMaxAge(maxAge);
        nettyCookie.setSecure(secured);
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public Response cookie(@NonNull String path, @NonNull String name, @NonNull String value, int maxAge, boolean secured) {
        Cookie nettyCookie = new io.netty.handler.codec.http.cookie.DefaultCookie(name, value);
        nettyCookie.setMaxAge(maxAge);
        nettyCookie.setSecure(secured);
        nettyCookie.setPath(path);
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public Response removeCookie(@NonNull String name) {
        Optional<Cookie> cookieOpt = this.cookies.stream().filter(cookie -> cookie.name().equals(name)).findFirst();
        cookieOpt.ifPresent(cookie -> {
            cookie.setValue("");
            cookie.setMaxAge(-1);
        });
        Cookie nettyCookie = new io.netty.handler.codec.http.cookie.DefaultCookie(name, "");
        nettyCookie.setMaxAge(-1);
        this.cookies.add(nettyCookie);
        return this;
    }

    @Override
    public Map<String, String> cookies() {
        Map<String, String> map = new HashMap<>(8);
        this.cookies.forEach(cookie -> map.put(cookie.name(), cookie.value()));
        return map;
    }

    @Override
    public void download(@NonNull String fileName, @NonNull File file) throws Exception {
        if (!file.exists() || !file.isFile()) {
            throw new NotFoundException("Not found file: " + file.getPath());
        }

        RandomAccessFile raf        = new RandomAccessFile(file, "r");
        Long             fileLength = raf.length();
        this.contentType = StringKit.mimeType(file.getName());

        io.netty.handler.codec.http.HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpHeaders                              httpHeaders  = httpResponse.headers().add(getDefaultHeader());

        boolean keepAlive = WebContext.request().keepAlive();
        if (keepAlive) {
            httpResponse.headers().set(HttpConst.CONNECTION, KEEP_ALIVE);
        }
        httpHeaders.set(HttpConst.CONTENT_TYPE, this.contentType);
        httpHeaders.set("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes("UTF-8"), "ISO8859_1"));
        httpHeaders.setInt(HttpConst.CONTENT_LENGTH, fileLength.intValue());

        // Write the initial line and the header.
        ctx.write(httpResponse);

        ChannelFuture sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
        // Write the end marker.
        ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        sendFileFuture.addListener(ProgressiveFutureListener.build(raf));
        // Decide whether to close the connection or not.
        if (!keepAlive) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
        isCommit = true;
    }

    @Override
    public OutputStreamWrapper outputStream() throws IOException {
        File         file         = Files.createTempFile("blade", ".temp").toFile();
        OutputStream outputStream = new FileOutputStream(file);
        return new OutputStreamWrapper(outputStream, file, ctx);
    }

    @Override
    public void render(@NonNull ModelAndView modelAndView) {
        StringWriter sw = new StringWriter();
        try {
            WebContext.chopsticks().templateEngine().render(modelAndView, sw);
            ByteBuf          buffer   = Unpooled.wrappedBuffer(sw.toString().getBytes("utf-8"));
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode), buffer);
            this.send(response);
        } catch (Exception e) {
            log.error("render error", e);
        }
    }

    @Override
    public void redirect(@NonNull String newUri) {
        headers.set(HttpConst.LOCATION, newUri);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        this.send(response);
    }

    @Override
    public boolean isCommit() {
        return isCommit;
    }

    @Override
    public void send(@NonNull FullHttpResponse response) {
        response.headers().set(getDefaultHeader());

        boolean keepAlive = WebContext.request().keepAlive();

        if (!response.headers().contains(HttpConst.CONTENT_LENGTH)) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(HttpConst.CONTENT_LENGTH, String.valueOf(response.content().readableBytes()));
        }

        if (!keepAlive) {
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(HttpConst.CONNECTION, KEEP_ALIVE);
            ctx.write(response, ctx.voidPromise());
        }
        isCommit = true;
    }

    private HttpHeaders getDefaultHeader() {
        headers.set(HttpConst.DATE, dateString);
        headers.set(HttpConst.CONTENT_TYPE, HttpConst.getContentType(this.contentType));
        headers.set(HttpConst.X_POWER_BY, HttpConst.VERSION);
        if (!headers.contains(HttpConst.SERVER)) {
            headers.set(HttpConst.SERVER, HttpConst.VERSION);
        }
        if (this.cookies.size() > 0) {
            this.cookies.forEach(cookie -> headers.add(HttpConst.SET_COOKIE, io.netty.handler.codec.http.cookie.ServerCookieEncoder.LAX.encode(cookie)));
        }
        return headers;
    }

    public HttpResponse(Response response){
        this.contentType = response.contentType();
        this.statusCode = response.statusCode();
        if(null != response.headers()){
            response.headers().forEach(this.headers::add);
        }
        if(null != response.cookies()){
            response.cookies().forEach( (k,v) -> this.cookies.add(new DefaultCookie(k, v)));
        }
    }

    public HttpResponse(){
    }

    public static HttpResponse build(ChannelHandlerContext ctx, CharSequence dateString) {
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.ctx = ctx;
        httpResponse.dateString = dateString;
        return httpResponse;
    }

}