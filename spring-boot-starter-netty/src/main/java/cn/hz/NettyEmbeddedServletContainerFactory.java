package cn.hz;

import cn.hz.core.NettyEmbeddedContext;
import cn.hz.core.NettyEmbeddedServletContainer;
import io.netty.bootstrap.Bootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import javax.servlet.ServletException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Random;

/**
 * An {@link EmbeddedServletContainerFactory} that can be used to create {@link NettyEmbeddedServletContainer}s.
 *
 */

public class NettyEmbeddedServletContainerFactory extends AbstractEmbeddedServletContainerFactory implements ResourceLoaderAware {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String SERVER_INFO = "Netty/SpringBoot";
    private ResourceLoader resourceLoader;

    @Override
    public EmbeddedServletContainer getEmbeddedServletContainer(ServletContextInitializer... initializers) {
        ClassLoader parentClassLoader = resourceLoader != null ? resourceLoader.getClassLoader() : ClassUtils.getDefaultClassLoader();

        Package nettyPackage = Bootstrap.class.getPackage();
        String title = nettyPackage.getImplementationTitle();
        String version = nettyPackage.getImplementationVersion();
        log.info("Running with " + title + " " + version);

        if (isRegisterDefaultServlet()) {
            log.warn("This container does not support a default servlet");
        }

        NettyEmbeddedContext context = new NettyEmbeddedContext(getContextPath(), new URLClassLoader(new URL[]{}, parentClassLoader), SERVER_INFO);
        for (ServletContextInitializer initializer : initializers) {
            try {
                log.info("ServletContextInitializer: " + initializer.getClass().getName());
                initializer.onStartup(context);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("server port from springboot confi: " + getPort());
        int port = getPort() > 0 ? getPort() : new Random().nextInt(65535 - 1024) + 1024;
        InetSocketAddress address = new InetSocketAddress(port);
        log.info("Server initialized with port: " + port);
        return new NettyEmbeddedServletContainer(address, context);
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
