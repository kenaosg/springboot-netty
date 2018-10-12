package cn.hz.common;

import cn.hz.core.NettyEmbeddedServletContainer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class AsyncHttpClient {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private Bootstrap bootstrap;
	private int timeout = 30 * 1000;//ms
	private int maxHttpSize = 8000;

	private static class InstanceHolder {
		private static final AsyncHttpClient INSTANCE = new AsyncHttpClient();
	}
	public static AsyncHttpClient getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private AsyncHttpClient() {
		EventLoopGroup workerGroup = NettyEmbeddedServletContainer.workerGroup;
		bootstrap = new Bootstrap();
		bootstrap.group(workerGroup);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
		bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ChannelInboundHandlerAdapter handler = new SimpleChannelInboundHandler<FullHttpResponse>() {

					@Override
					protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
						// TODO Auto-generated method stub
						Callback cb = ctx.channel().attr(KEY_CB).get();
						FullHttpRequest request = ctx.channel().attr(KEY_REQ).get();
						ChannelHandlerContext serCtx = ctx.channel().attr(KEY_CTX).get();
						log.info("recv http response [" + request.uri() + "] " + response.status());
						if(serCtx == null) {
							cb.onResponse(response);
						} else {
							cb.onResponse(request, response, serCtx);
						}
						
						ctx.channel().close();
					}

					@Override
					public void userEventTriggered(ChannelHandlerContext ctx,
							Object evt) throws Exception {
						if (evt instanceof IdleStateEvent) {
							Callback cb = ctx.channel().attr(KEY_CB).get();
							FullHttpRequest request = ctx.channel().attr(KEY_REQ).get();
							log.warn("http request[" + request.uri() + "] timeout.");
							cb.onTimeout();
							ctx.channel().close();
						}
					}
					
					@Override
					public void channelInactive(ChannelHandlerContext ctx) throws Exception {
				    	//Boolean descrease = ctx.channel().attr(Constants.decreaseConnPayLoad).get();
//				    	if (descrease != null && !descrease) {
//				    		//ctx.channel().attr(Constants.decreaseConnPayLoad).set(true);
//				    	}
				    	ctx.close();
					}
					
					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
						cause.printStackTrace();
					}
				};

				ch.pipeline()
						.addLast(new HttpResponseDecoder())
						.addLast(new HttpRequestEncoder())
						.addLast(new HttpObjectAggregator(getMaxHttpSize()))
						.addLast(new IdleStateHandler(getTimeout(), getTimeout(), getTimeout(), TimeUnit.MILLISECONDS))
						.addLast(handler);
			}
		});
	}

	public static interface Callback {
		public void onResponse(FullHttpResponse response);
		public void onResponse(FullHttpRequest request, FullHttpResponse response, ChannelHandlerContext ctx);
		public void onTimeout();
		public void onConnectFail();
	}

	public static abstract class CallbackAdapter implements Callback {
		@Override
		public void onResponse(FullHttpResponse response) {

		}

		@Override
		public abstract void onResponse(FullHttpRequest request, FullHttpResponse response,
										ChannelHandlerContext ctx);

		@Override
		public void onTimeout() {

		}

		@Override
		public void onConnectFail() {

		}
	}

	final static AttributeKey<Callback> KEY_CB = AttributeKey.valueOf("cb");
	final static AttributeKey<FullHttpRequest> KEY_REQ = AttributeKey.valueOf("req");
	final static AttributeKey<ChannelHandlerContext> KEY_CTX = AttributeKey.valueOf("ctx");
	
	public void sendRequest(final String host, final int port,
			final FullHttpRequest request, final ChannelHandlerContext ctx, final Callback cb) {
		
		log.info("send http request[" + request.uri() + "] to " + host + ":" + port);
				
		bootstrap.connect(host, port).addListener(new ChannelFutureListener() {

			public void operationComplete(ChannelFuture f) {
				// TODO Auto-generated method stub
				if (f.isSuccess()) {
					request.headers().set("Host", host + ":" + port);
					f.channel().attr(KEY_CB).set(cb);
					f.channel().attr(KEY_REQ).set(request);
					f.channel().attr(KEY_CTX).set(ctx);
					f.channel().writeAndFlush(request);
				} else {
					log.warn("connect to " + host + ":" + port + " failed.");
					//request.release();
					cb.onConnectFail();
				}
			}
		});
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getMaxHttpSize() {
		return maxHttpSize;
	}

	public void setMaxHttpSize(int maxHttpSize) {
		this.maxHttpSize = maxHttpSize;
	}
	
	public static void main(String[] args) {

		//PropertyConfigurator.configure("conf/log4j.properties");

//		AsyncHttpClient client = new AsyncHttpClient();
//		DefaultFullHttpRequest request = new DefaultFullHttpRequest(
//				HttpVersion.HTTP_1_1, HttpMethod.GET, "/AsyncHttpClient");
//		client.sendRequest("127.0.0.1", 8888, request, null, null, new Callback() {
//
//			public void onResponse(FullHttpResponse response) {
//				// TODO Auto-generated method stub
//				System.out.println("AsyncHttpClient onResponse");
//			}
//
//			public void onTimeout() {
//				// TODO Auto-generated method stub
//				System.out.println("AsyncHttpClient onTimeout");
//			}
//
//			public void onConnectFail() {
//				// TODO Auto-generated method stub
//				System.out.println("AsyncHttpClient onConnectFail");
//			}
//
//			@Override
//			public void onResponse(FullHttpRequest request,
//					FullHttpResponse response, MsgSrc source,
//					ChannelHandlerContext ctx) {
//				// TODO Auto-generated method stub
//
//			}
//		});
	}

}

