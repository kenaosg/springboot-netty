package cn.hz.core;

import cn.hz.NettyEmbeddedServletContainerFactory;
import com.google.common.base.StandardSystemProperty;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This is a minimal Servlet 3.1 implementation to provide for the opinionated embedded servlet container model for
 * Spring Boot, supporting a single context, runtime {@link javax.servlet.Registration} only, and no default or JSP
 * servlets.
 * <p>
 * This class should be created using the {@link NettyEmbeddedServletContainerFactory}.
 *
 */

public class NettyEmbeddedServletContainer implements EmbeddedServletContainer {
    private final Log log = LogFactory.getLog(getClass());

    private final InetSocketAddress address;
    private final NettyEmbeddedContext servletContext;

    private EventLoopGroup bossGroup;
    public static EventLoopGroup workerGroup;
    private DefaultEventExecutorGroup servletExecutor;

    public NettyEmbeddedServletContainer(InetSocketAddress address, NettyEmbeddedContext servletContext) {
        this.address = address;
        this.servletContext = servletContext;
    }

    @Override
    public void start() throws EmbeddedServletContainerException {

        ServerBootstrap sb = new ServerBootstrap();
        groups(sb);

        servletExecutor = new DefaultEventExecutorGroup(50);
        sb.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("decoder", new HttpRequestDecoder());
                pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                pipeline.addLast("encoder", new HttpResponseEncoder());
                pipeline.addLast("idleState", new IdleStateHandler(10 * 1000, 10 * 1000, 10 * 1000, TimeUnit.MILLISECONDS));
                pipeline.addLast("servletInput", new ServletContentHandler(servletContext)); //处理请求，读入数据，生成Request和Response对象
                pipeline.addLast(checkNotNull(servletExecutor), "filterChain", new RequestDispatcherHandler(servletContext));
            }
        });

        // Don't yet need the complexity of lifecycle state, listeners etc, so tell the context it's initialised here
        servletContext.setInitialised(true);

        ChannelFuture future = sb.bind(address).awaitUninterruptibly();
        Throwable cause = future.cause();
        if (null != cause) {
            throw new EmbeddedServletContainerException("Could not start Netty server", cause);
        }
        log.info(servletContext.getServerInfo() + " started on port: " + getPort());
    }

    private void groups(ServerBootstrap b) {
        if (StandardSystemProperty.OS_NAME.value().equals("Linux")) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup();
            b.channel(EpollServerSocketChannel.class)
                    .group(bossGroup, workerGroup)
                    .option(EpollChannelOption.TCP_CORK, true);
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            b.channel(NioServerSocketChannel.class)
                    .group(bossGroup, workerGroup);
        }
        b.option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 100);
        log.info("Bootstrap configuration: " + b.toString());
    }

    @Override
    public void stop() throws EmbeddedServletContainerException {
        log.info("shutting down NettyEmbeddedServletContainer...");
        try {
            if (null != bossGroup) {
                bossGroup.shutdownGracefully().await();
            }
            if (null != workerGroup) {
                workerGroup.shutdownGracefully().await();
            }
            if (null != servletExecutor) {
                servletExecutor.shutdownGracefully().await();
            }
        } catch (InterruptedException e) {
            throw new EmbeddedServletContainerException("Container stop interrupted", e);
        }
    }

    @Override
    public int getPort() {
        return address.getPort();
    }
}
