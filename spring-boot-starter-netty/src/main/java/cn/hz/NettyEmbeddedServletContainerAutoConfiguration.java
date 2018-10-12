package cn.hz;

import io.netty.bootstrap.Bootstrap;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnWebApplication
public class NettyEmbeddedServletContainerAutoConfiguration {
    @Configuration
    @ConditionalOnClass({Bootstrap.class})
    @ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, search = SearchStrategy.CURRENT)
    public static class EmbeddedNetty {
        @Bean
        public NettyEmbeddedServletContainerFactory nettyEmbeddedServletContainerFactory() {
            return new NettyEmbeddedServletContainerFactory();
        }
    }
}
