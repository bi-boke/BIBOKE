package org.bok.core.services.http;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bok.api.GrpcAPI.NumberMessage;
import org.bok.core.Wallet;
import org.bok.protos.Protocol.Block;


@Component
@Slf4j(topic = "API")
public class GetBlockByNumServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      fillResponse(Util.getVisible(request), Long.parseLong(request.getParameter("num")), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      boolean visible = Util.getVisiblePost(input);
      NumberMessage.Builder build = NumberMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      fillResponse(visible, build.getNum(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible, long num, HttpServletResponse response)
      throws IOException {
    Block reply = wallet.getBlockByNum(num);
    if (reply != null) {
      response.getWriter().println(Util.printBlock(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}