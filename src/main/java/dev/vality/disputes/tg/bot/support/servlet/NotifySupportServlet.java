package dev.vality.disputes.tg.bot.support.servlet;

import dev.vality.disputes.admin.AdminCallbackServiceSrv;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@WebServlet("/v1/support")
public class NotifySupportServlet extends GenericServlet {

    @Autowired
    private AdminCallbackServiceSrv.Iface disputesApiNotificationHandler;

    private Servlet servlet;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        servlet = new THServiceBuilder()
                .build(AdminCallbackServiceSrv.Iface.class, disputesApiNotificationHandler);
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        servlet.service(request, response);
    }
}
