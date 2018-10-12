package cn.hz.core;

import cn.hz.common.AsyncHttpClient;
import cn.hz.common.SpringmvcNettyConstantsAll;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;

import javax.servlet.http.HttpServletRequest;

public class HttpTransmitProxy {

    /**
     * transmit request to specified host:port, then response will be transmitted back to the
     * original client automatically
     * @param host
     * @param port
     * @param request: use netty FullHttpRequest
     * @param ctx: get it by method getAttribute in HttpServletRequest.
     * @param servletRequest
     */
    public static void transmit(final String host, final int port, final FullHttpRequest request,
                                final ChannelHandlerContext ctx, HttpServletRequest servletRequest) {

        servletRequest.setAttribute(SpringmvcNettyConstantsAll.SPRINGMVC_CONTROLLER_MODE,
                SpringmvcNettyConstantsAll.SPRINGMVC_CONTROLLER_MODE_TRANSMIT);

        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());

        AsyncHttpClient.getInstance().sendRequest(host, port, request, ctx, new AsyncHttpClient.CallbackAdapter() {
            @Override
            public void onResponse(FullHttpRequest request, FullHttpResponse response, ChannelHandlerContext ctx) {
                response.content().retain();
                ctx.channel().writeAndFlush(response);//return to original client before transmit
            }
        });
    }

    public static void transmitPostHandle(final String host, final int port, final FullHttpRequest request,
                                          final ChannelHandlerContext ctx, final AsyncHttpClient.Callback postHandleCb,
                                          HttpServletRequest servletRequest) {

        servletRequest.setAttribute(SpringmvcNettyConstantsAll.SPRINGMVC_CONTROLLER_MODE,
                SpringmvcNettyConstantsAll.SPRINGMVC_CONTROLLER_MODE_TRANSMIT);
        AsyncHttpClient.getInstance().sendRequest(host, port, request, ctx, postHandleCb);
    }
}
