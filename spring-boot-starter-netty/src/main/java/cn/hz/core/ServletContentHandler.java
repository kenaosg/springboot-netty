package cn.hz.core;

import cn.hz.common.GetDetailOfException;
import cn.hz.common.HttpParseUtil;
import cn.hz.common.SpringmvcNettyConstantsAll;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class ServletContentHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final Log log = LogFactory.getLog(getClass());

    private NettyEmbeddedContext servletContext;
    private static final AttributeKey<String> SOCKET_CHANNEL_IDENTITY = AttributeKey.valueOf("sci");

    ServletContentHandler(NettyEmbeddedContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest){
        if (!fullHttpRequest.decoderResult().isSuccess()) {
            sendError(channelHandlerContext, BAD_REQUEST);
            return;
        }

        try {

            MockHttpServletRequest servletRequest = createServletRequest(fullHttpRequest);

            //for async/transmit use
            servletRequest.setAttribute(SpringmvcNettyConstantsAll.NETTY_CHANNEL_HANDLER_CONTEXT, channelHandlerContext);
            servletRequest.setAttribute(SpringmvcNettyConstantsAll.SPRINGMVC_CONTROLLER_MODE, SpringmvcNettyConstantsAll.SPRINGMVC_CONTROLLER_MODE_NORMAL);

            channelHandlerContext.fireChannelRead(servletRequest);
        } catch (Exception e) {
            log.error(GetDetailOfException.getDetailOfException(e));
            sendError(channelHandlerContext, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx,
                                   Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.info("user event triggered: " + ((IdleStateEvent) evt).state().name() +
                    ", channel identity: " + ctx.channel().attr(SOCKET_CHANNEL_IDENTITY).get());
            ctx.channel().close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        log.info("channel inactive, channel identity: " +
                ", channel identity: " + ctx.channel().attr(SOCKET_CHANNEL_IDENTITY).get());
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(GetDetailOfException.getDetailOfThrowable(cause));
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }else {
            log.info("channel inactive, channel identity: " +
                    ", channel identity: " + ctx.channel().attr(SOCKET_CHANNEL_IDENTITY).get());
            ctx.close();
        }
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        ByteBuf content = Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8);

        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HTTP_1_1, status, content);
        fullHttpResponse.headers().add(CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.write(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
    }

    private MockHttpServletRequest createServletRequest(FullHttpRequest fullHttpRequest) {

        String fullUri = fullHttpRequest.uri();
        String uri;
        String queryStr;
        int index = fullUri.indexOf("?");
        if(index < 0) {
            uri = fullUri;
            queryStr = null;
        } else {
            uri = fullUri.substring(0, index);
            queryStr = fullUri.substring(index+1);
        }

        MockHttpServletRequest servletRequest = new MockHttpServletRequest(this.servletContext);
        servletRequest.setMethod(fullHttpRequest.method().name());
        servletRequest.setRequestURI(uri);
        servletRequest.setProtocol(fullHttpRequest.protocolVersion().text());

        HttpHeaders httpHeaders = fullHttpRequest.headers();
        servletRequest.setContentType(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE));
        servletRequest.setCharacterEncoding(httpHeaders.get(HttpHeaderNames.CONTENT_TRANSFER_ENCODING));
        servletRequest.setAuthType(httpHeaders.get(HttpHeaderNames.AUTHORIZATION));
        servletRequest.setQueryString(queryStr);
//		servletRequest.setScheme();
//		servletRequest.setServerName();
//		servletRequest.setServerPort();
        servletRequest.setContextPath("");
        servletRequest.setServletPath(uri);
        servletRequest.setPathInfo(null);
//		servletRequest.setLocalAddr();
//		servletRequest.setLocalName();
//		servletRequest.setLocalPort();
//		servletRequest.setRemoteAddr();
//		servletRequest.setRemoteHost();
//		servletRequest.setRemotePort();
//		servletRequest.setRemoteUser();

        if(HttpMethod.GET.equals(fullHttpRequest.method())) {
            servletRequest.setParameters(HttpParseUtil.getRequestParamsFromUri(queryStr));
        } else {
            ByteBuf byteBuf = fullHttpRequest.content();
            byte[] bytes;
            if(byteBuf == null){
                bytes = null;
            }else if (byteBuf.hasArray()) {
                bytes = byteBuf.array();
            } else {
                bytes = new byte[byteBuf.capacity()];
                byteBuf.getBytes(0, bytes, 0, byteBuf.capacity());
            }

            if(bytes != null) {
                //application/x-www-form-urlencoded
                if (HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString().equalsIgnoreCase(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE))) {
                    //Map<String, String[]>
                    servletRequest.setParameters(HttpParseUtil.getRequestParamsFromPostBytes(bytes));
                } else {
                    servletRequest.setContent(bytes);
                }
            }
        }

        return servletRequest;
    }
}