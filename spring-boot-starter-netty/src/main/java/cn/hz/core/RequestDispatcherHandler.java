package cn.hz.core;

import cn.hz.common.GetDetailOfException;
import cn.hz.common.SpringmvcNettyConstantsAll;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@ChannelHandler.Sharable
class RequestDispatcherHandler extends SimpleChannelInboundHandler<HttpServletRequest> {
    private final Log log = LogFactory.getLog(getClass());

    private final NettyEmbeddedContext context;
    private static final AttributeKey<String> SOCKET_CHANNEL_IDENTITY = AttributeKey.valueOf("sci");

    RequestDispatcherHandler(NettyEmbeddedContext context) {
        this.context = checkNotNull(context);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpServletRequest servletRequest) throws Exception {
        try {
            NettyRequestDispatcher dispatcher = (NettyRequestDispatcher) context.getRequestDispatcher(servletRequest.getRequestURI());
            if (dispatcher == null) {
                sendError(channelHandlerContext, HttpResponseStatus.NOT_FOUND);
                return;
            }

            MockHttpServletResponse servletResponse = new MockHttpServletResponse();
            dispatcher.dispatch(servletRequest, servletResponse);

            if(servletRequest.getAttribute("name") != null) {
                channelHandlerContext.channel().attr(SOCKET_CHANNEL_IDENTITY).set(servletRequest.getAttribute("name").toString());
            }

            if(servletRequest.getAttribute(SpringmvcNettyConstantsAll.SPRINGMVC_CONTROLLER_MODE).toString()
                    .equals(SpringmvcNettyConstantsAll.SPRINGMVC_CONTROLLER_MODE_TRANSMIT)) {
                //transmit, refer to HttpTransmitProxy
                //pay attention to timeout of upstream and downstream

            } else if(servletRequest.getAttribute(SpringmvcNettyConstantsAll.SPRINGMVC_CONTROLLER_MODE).toString()
                    .equals(SpringmvcNettyConstantsAll.SPRINGMVC_CONTROLLER_MODE_ASYNC)){
                //async, refer to HttpAsyncProxy
                //pay attention to timeout of upstream and downstream

            } else {
                //normal controller, like SpringMVC controller
                byte[] contentByte = servletResponse.getContentAsByteArray();
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                        HttpResponseStatus.valueOf(servletResponse.getStatus()),
                        Unpooled.wrappedBuffer(contentByte));
                for (String name : servletResponse.getHeaderNames()) {
                    for (Object value : servletResponse.getHeaderValues(name)) {
                        response.headers().add(name, value);
                    }
                }
                if(!response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
                    response.headers().add(HttpHeaderNames.CONTENT_LENGTH, contentByte.length);
                }

                ChannelFuture writeFuture = channelHandlerContext.channel().writeAndFlush(response);
//			writeFuture.addListener(new ChannelFutureListener() {
//				@Override
//				public void operationComplete(ChannelFuture channelFuture) throws Exception {
//
//				}
//			});

                writeFuture.addListener(channelFuture -> {
                    log.debug("write is done: " + channelFuture.isDone());
                    log.debug("write is success: " + channelFuture.isSuccess());
//				log.debug("write is cancelled: " + channelFuture.isCancelled());
                    channelHandlerContext.channel().close();

                });

//			writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
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
}
