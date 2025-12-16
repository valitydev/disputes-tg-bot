package dev.vality.disputes.tg.bot.servlet;

import dev.vality.disputes.provider.ProviderDisputesServiceSrv;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@WebServlet("/v1/provider")
public class DefaultProviderDisputesServlet extends GenericServlet {

    @Autowired
    private ProviderDisputesServiceSrv.Iface providerDisputesService;

    private Servlet servlet;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        servlet = new THServiceBuilder()
                .build(ProviderDisputesServiceSrv.Iface.class, providerDisputesService);
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        servlet.service(request, response);
    }
}
