package com.chopsticks.mvc.http;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.chopsticks.kit.JsonKit;
import com.chopsticks.mvc.Const;
import com.chopsticks.mvc.WebContext;
import com.chopsticks.mvc.ui.ModelAndView;
import com.chopsticks.mvc.wrapper.OutputStreamWrapper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public interface Response {
    /**
     * Get current response http status code. e.g: 200
     *
     * @return return response status code
     */
    int statusCode();

    /**
     * Setting Response Status
     *
     * @param status
     *            status code
     * @return Return Response
     */
    Response status(int status);

    /**
     * Set current response http code 400
     *
     * @return Setting Response Status is BadRequest and Return Response
     */
    default Response badRequest() {
        return status(400);
    }

    /**
     * Set current response http code 401
     *
     * @return Setting Response Status is unauthorized and Return Response
     */
    default Response unauthorized() {
        return status(401);
    }

    /**
     * Set current response http code 404
     *
     * @return Setting Response Status is notFound and Return Response
     */
    default Response notFound() {
        return status(404);
    }

    /**
     * Setting Response ContentType
     *
     * @param contentType
     *            content type
     * @return Return Response
     */
    Response contentType(CharSequence contentType);

    /**
     * Get current response headers: contentType
     *
     * @return return response content-type
     */
    String contentType();

    /**
     * Get current response headers
     *
     * @return return response headers
     */
    Map<String , String> headers();

    /**
     * Set current response header
     *
     * @param name
     *            Header Name
     * @param value
     *            Header Value
     * @return Return Response
     */
    Response header(CharSequence name, CharSequence value);

    /**
     * Get current response cookies
     *
     * @return return response cookies
     */
    Map<String , String> cookies();

    /**
     * add raw response cookie
     *
     * @param cookie
     * @return return Response instance
     */
    Response cookie(Cookie cookie);

    /**
     * add Cookie
     *
     * @param name
     *            Cookie Name
     * @param value
     *            Cookie Value
     * @return Return Response
     */
    Response cookie(String name, String value);

    /**
     * Setting Cookie
     *
     * @param name
     *            Cookie Name
     * @param value
     *            Cookie Value
     * @param maxAge
     *            Period of validity
     * @return Return Response
     */
    Response cookie(String name, String value, int maxAge);

    /**
     * Setting Cookie
     *
     * @param name
     *            Cookie Name
     * @param value
     *            Cookie Value
     * @param maxAge
     *            Period of validity
     * @param secured
     *            Is SSL
     * @return Return Response
     */
    Response cookie(String name, String value, int maxAge, boolean secured);

    /**
     * Setting Cookie
     *
     * @param path
     *            Cookie Domain Path
     * @param name
     *            Cookie Name
     * @param value
     *            Cookie Value
     * @param maxAge
     *            Period of validity
     * @param secured
     *            Is SSL
     * @return Return Response
     */
    Response cookie(String path, String name, String value, int maxAge, boolean secured);

    /**
     * remove cookie
     *
     * @param name
     * @return return Response instance
     */
    Response removeCookie(String name);

    /**
     * Render by text
     *
     * @param text
     *            text content
     */
    default void text(String text) {
        if (null == text) return;
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode()), Unpooled.wrappedBuffer(text.getBytes(CharsetUtil.UTF_8)), false);
        if (null == this.contentType()) this.contentType(Const.CONTENT_TYPE_TEXT);
        this.send(response);
    }

    /**
     * Render by html
     *
     * @param html
     *            html content
     */
    default void html(String html) {
        if (null == html) return;
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode()), Unpooled.wrappedBuffer(html.getBytes(CharsetUtil.UTF_8)), false);
        if (null == this.contentType()) this.contentType(Const.CONTENT_TYPE_HTML);
        this.send(response);
    }

    /**
     * Render by json
     *
     * @param json
     *            json content
     */
    default void json(String json) {
        if (null == json) return;
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode()), Unpooled.wrappedBuffer(json.getBytes(CharsetUtil.UTF_8)), false);
        if (null == this.contentType() && !WebContext.request().isIE()) {
            this.contentType(Const.CONTENT_TYPE_JSON);
        }
        this.send(response);
    }

    /**
     * Render by json
     *
     * @param bean
     *            bean instance
     */
    default void json(Object bean) {
        if (null == bean) return;
        this.json(JsonKit.toString(bean));
    }

    /**
     * send body to client
     *
     * @param data
     *            body string
     */
    default void body(String data) {
        if (null == data) return;
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode()), Unpooled.wrappedBuffer(data.getBytes(CharsetUtil.UTF_8)), false);
        this.send(response);
    }

    /**
     * Send response body by byte array.
     *
     * @param data
     *            byte array data
     */
    default void body(byte[] data) {
        if (null == data) return;
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode()), Unpooled.wrappedBuffer(data), false);
        this.send(response);
    }

    /**
     * Send response body by ByteBuf.
     *
     * @param byteBuf
     *            ByteBuf data
     */
    default void body(ByteBuf byteBuf) {
        if (null == byteBuf) return;
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode()), byteBuf, false);
        this.send(response);
    }

    /**
     * download some file to client
     *
     * @param fileName
     *            give client file name
     * @param file
     *            file storage location
     */
    void download(String fileName, File file) throws Exception;

    /**
     * create temp file outputStream
     *
     * @return return OutputStreamWrapper
     * @throws IOException
     *             throw IOException
     * @since 2.0.1-alpha3
     */
    OutputStreamWrapper outputStream() throws IOException;

    /**
     * Render view
     *
     * @param view
     *            view page
     * @return Return Response
     */
    default void render(String view) {
        if (null == view) return;
        this.render(new ModelAndView(view));
    }

    /**
     * Render view And Setting Data
     *
     * @param modelAndView
     *            ModelAndView object
     * @return Return Response
     */
    void render(ModelAndView modelAndView);

    /**
     * Redirect to newUri
     *
     * @param newUri
     *            new url
     */
    void redirect(String newUri);

    /**
     * Judge whether the current response has been submitted to the client
     *
     * @return return current response is commit
     */
    boolean isCommit();

    /**
     * Send response by FullHttpResponse, custom build, please be careful
     *
     * @param response
     *            FullHttpResponse instance
     */
    void send(FullHttpResponse response);
}
