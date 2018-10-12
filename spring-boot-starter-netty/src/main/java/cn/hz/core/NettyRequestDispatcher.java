package cn.hz.core;

import javax.servlet.*;
import java.io.IOException;

public class NettyRequestDispatcher implements RequestDispatcher {
    private final ServletContext context;
    private final FilterChain filterChain;

    NettyRequestDispatcher(ServletContext context, FilterChain filterChain) {
        this.context = context;
        this.filterChain = filterChain;
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        request.setAttribute("NETTY_DISPATCHER_TYPE", DispatcherType.FORWARD);
        // TODO implement
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        request.setAttribute("NETTY_DISPATCHER_TYPE", DispatcherType.INCLUDE);
        // TODO implement
    }

    public void dispatch(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        request.setAttribute("NETTY_DISPATCHER_TYPE", DispatcherType.ASYNC);
        filterChain.doFilter(request, response);
    }
}
