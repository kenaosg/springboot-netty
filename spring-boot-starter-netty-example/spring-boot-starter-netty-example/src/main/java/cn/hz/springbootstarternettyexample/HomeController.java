package cn.hz.springbootstarternettyexample;

import cn.hz.common.SpringmvcNettyConstantsAll;
import cn.hz.core.HttpTransmitProxy;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping(path="/home")
public class HomeController {

    private static Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private DiscoveryClient client;

    //curl -i -v http://127.0.0.1:11000/home/normal \
    //-H "Content-Type:application/json" \
    //-X POST \
    //-d '{"glossary":{"title":"example glossary","GlossDiv":{"title":"S","GlossList":{"GlossEntry":{"ID":"SGML","SortAs":"SGML","GlossTerm":"Standard Generalized Markup Language","Acronym":"SGML","Abbrev":"ISO 8879:1986","GlossDef":{"para":"A meta-markup language, used to create markup languages such as DocBook.","GlossSeeAlso":["GML","XML"]},"GlossSee":"markup"}}}}}'
    @RequestMapping(value = "/normal")
    public void normal(HttpServletRequest request, HttpServletResponse response) throws Exception{
        log.info("Request url is: {}", request.getRequestURL());
        InputStream inputStream = request.getInputStream();
        byte[] bytes = new byte[request.getContentLength()];
        inputStream.read(bytes, 0, bytes.length);
        log.info("Request body is: {}", new String(bytes, "utf-8"));

        try {
            String strReJson = "hello, I'm normal\r\n";
            response.setContentType("text/plain");
            response.getWriter().print(strReJson);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //curl -i -v http://127.0.0.1:11000/home/transmit \
    //-H "Content-Type:application/json" \
    //-X POST \
    //-d '{"glossary":{"title":"example glossary","GlossDiv":{"title":"S","GlossList":{"GlossEntry":{"ID":"SGML","SortAs":"SGML","GlossTerm":"Standard Generalized Markup Language","Acronym":"SGML","Abbrev":"ISO 8879:1986","GlossDef":{"para":"A meta-markup language, used to create markup languages such as DocBook.","GlossSeeAlso":["GML","XML"]},"GlossSee":"markup"}}}}}'
    @RequestMapping(value = "/transmit")
    public void transmit(HttpServletRequest request, HttpServletResponse response) throws Exception{
        log.info("Request url is: {}", request.getRequestURL());
        InputStream inputStream = request.getInputStream();
        byte[] bytes = new byte[request.getContentLength()];
        inputStream.read(bytes, 0, bytes.length);
        log.info("Request body is: {}", new String(bytes, "utf-8"));

        //eureka start
        log.info(client.getServices().toString());
        List<ServiceInstance> serviceInstanceList = client.getInstances("HOME-SERVICE");
        for(ServiceInstance serviceInstance : serviceInstanceList) {
            log.info(serviceInstance.getHost());
            log.info(serviceInstance.getPort() + "");
//            log.info(serviceInstance.getUri().getPath());
//            log.info(serviceInstance.getServiceId());
//            log.info(serviceInstance.getMetadata().toString());
        }
        //eureka end

        //ribbon start
        ApplicationContext context = SpringBootStarterNettyExampleApplication.applicationContext;
        SpringClientFactory springClientFactory = context.getBean(SpringClientFactory.class);
        ILoadBalancer loadBalancer = springClientFactory.getLoadBalancer("HOME-SERVICE");

        Server server = loadBalancer.chooseServer(null);
        log.info(server != null ? server.toString() : "no server selectable");
        //ribbon end

        ChannelHandlerContext channelHandlerContext = (ChannelHandlerContext) request.getAttribute(
                SpringmvcNettyConstantsAll.NETTY_CHANNEL_HANDLER_CONTEXT);
        FullHttpRequest fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
                "/home/normal", Unpooled.wrappedBuffer("transmit text".getBytes()));

        HttpTransmitProxy.transmit(server.getHost(),
                server.getPort(),
                fullHttpRequest,
                channelHandlerContext,
                request);
    }

    @RequestMapping(value = "/async")
    public void async(HttpServletRequest request, HttpServletResponse response) throws Exception{
        log.info("Request url is: {}", request.getRequestURL());
        InputStream inputStream = request.getInputStream();
        byte[] bytes = new byte[request.getContentLength()];
        inputStream.read(bytes, 0, bytes.length);
        log.info("Request body is: {}", new String(bytes, "utf-8"));

        //TODO
        //send several http requests to several servers, wait them all completed non-blocking,
        //handle all response when the last request completed
    }

}
