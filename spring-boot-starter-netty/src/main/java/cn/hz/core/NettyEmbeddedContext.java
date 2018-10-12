package cn.hz.core;

import cn.hz.registration.NettyFilterRegistration;
import cn.hz.registration.NettyServletRegistration;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Netty servlet bridge {@link ServletContext} implementation.
 */
public class NettyEmbeddedContext implements ServletContext {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String contextPath;
    private final ClassLoader classLoader;
    private final String serverInfo;
    private volatile boolean initialized;

    private final Map<String, NettyServletRegistration> servlets = new HashMap<>(); //used in getServletRegistration(), key is ServletName
    private final Map<String, String> servletMappings = new HashMap<>(); //保存请求路径urlPattern与Servlet名的映射,urlPattern不带contextPath
    private final Map<String, NettyFilterRegistration> filters = new HashMap<>(); //used in getFilterRegistration(), Key is FilterName

    private final Hashtable<String, Object> attributes = new Hashtable<>();

    public NettyEmbeddedContext(String contextPath, ClassLoader classLoader, String serverInfo) {

        //contextPath, default is ""
        //can be set by property "server.contextPath"(before 2.0 version) or "server.servlet.contextPath"
        if(contextPath.endsWith("/")){
            contextPath = contextPath.substring(0, contextPath.length() -1);
        }
        log.info("contextPath : " + contextPath);

        this.contextPath = contextPath;
        this.classLoader = classLoader;
        this.serverInfo = serverInfo;

    }

    void setInitialised(boolean initialized) {
        this.initialized = initialized;
    }

    private boolean isInitialised() {
        return initialized;
    }

    private void checkNotInitialised() {
        checkState(!isInitialised(), "This method can not be called before the context has been initialised");
    }

    public void addServletMapping(String urlPattern, String name, Servlet servlet) throws ServletException {
        checkNotInitialised();
        servletMappings.put(urlPattern, checkNotNull(name));
    }

    public void addFilterMapping(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String urlPattern) {
        checkNotInitialised();
        //TODO 过滤器的urlPatter解析
    }

    /**
     * SpringBoot只有一个Context，我觉得直接返回this就可以了
     */
    @Override
    public ServletContext getContext(String uripath) {
        return this;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public int getMajorVersion() {
        return 3;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 3;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String file) {
        return null;
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        return null;
    }

    @Override
    public URL getResource(String path) {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        // FIXME proper path matching
        // very simple, but ok for now
        String servletName = servletMappings.get(path);
        if (servletName == null) {
            servletName = servletMappings.get("/");
        }
        Servlet servlet;
        try {
            servlet = null == servletName ? null : servlets.get(servletName).getServlet();
            if (servlet == null) {
                return null;
            }
            //TODO 过滤器的urlPatter解析
            List<Filter> allNeedFilters = new ArrayList<>();
            for (NettyFilterRegistration registration : this.filters.values()) {
                allNeedFilters.add(registration.getFilter());
            }
            FilterChain filterChain = new NettyFilterChain(servlet, allNeedFilters);
            return new NettyRequestDispatcher(this, filterChain);
        } catch (ServletException e) {
            log.error("Throwing exception when getting Filter from NettyFilterRegistration of path " + path, e);
            return null;
        }
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        return servlets.get(name).getServlet();
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        return Collections.emptyEnumeration();
    }

    @Override
    public Enumeration<String> getServletNames() {
        return Collections.emptyEnumeration();
    }

    @Override
    public void log(String msg) {
        log.info(msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        log.error(msg, exception);
    }

    @Override
    public void log(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    @Override
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public String getServerInfo() {
        return serverInfo;
    }

    @Override
    public String getInitParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.emptyEnumeration();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return false;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return attributes.keys();
    }

    @Override
    public void setAttribute(String name, Object object) {
         attributes.put(name, object);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public String getServletContextName() {
        return getContextPath().toUpperCase(Locale.ENGLISH);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        return addServlet(servletName, className, null);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        return addServlet(servletName, servlet.getClass().getName(), servlet);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        return addServlet(servletName, servletClass.getName());
    }

    private ServletRegistration.Dynamic addServlet(String servletName, String className, Servlet servlet) {
        NettyServletRegistration servletRegistration = new NettyServletRegistration(this, servletName, className, servlet);
        servlets.put(servletName, servletRegistration);
        return servletRegistration;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> c) throws ServletException {
        try {
            return c.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
           log.error("Throwing exception when creating instance of " + c.getName(), e);
        }
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return addFilter(filterName, className, null);
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return addFilter(filterName, filter.getClass().getName(), filter);
    }

    private javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className, Filter filter) {
        NettyFilterRegistration filterRegistration = new NettyFilterRegistration(this, filterName, className, filter);
        filters.put(filterName, filterRegistration);
        return filterRegistration;
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return addFilter(filterName, filterClass.getName());
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> c) throws ServletException {
        try {
            return c.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public javax.servlet.FilterRegistration getFilterRegistration(String filterName) {
        return filters.get(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return ImmutableMap.copyOf(filters);
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) throws IllegalStateException, IllegalArgumentException {

    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }


    //TODO 暂不支持Listener，现在很少用了吧
    @Override
    public void addListener(String className) {

    }

    @Override
    public <T extends EventListener> void addListener(T t) {

    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {

    }

    @Override
    public <T extends EventListener> T createListener(Class<T> c) throws ServletException {
        return null;
    }

    @Override
    public void declareRoles(String... roleNames) {

    }

    @Override
    public String getVirtualServerName() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }
}
