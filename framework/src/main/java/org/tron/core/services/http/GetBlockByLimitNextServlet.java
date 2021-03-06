package org.bok.core.services.http;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bok.api.GrpcAPI.BlockLimit;
import org.bok.api.GrpcAPI.BlockList;
import org.bok.core.Wallet;


@Component
@Slf4j(topic = "API")
public class GetBlockByLimitNextServlet extends RateLimiterServlet {

  private static final long BLOCK_LIMIT_NUM = 100;
  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      fillResponse(Util.getVisible(request), Long.parseLong(request.getParameter("startNum")),
          Long.parseLong(request.getParameter("endNum")), response);
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
      BlockLimit.Builder build = BlockLimit.newBuilder();
      JsonFormat.merge(input, build, visible);
      fillResponse(visible, build.getStartNum(), build.getEndNum(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible, long startNum, long endNum,
      HttpServletResponse response)
      throws IOException {
    if (endNum > 0 && endNum > startNum && endNum - startNum <= BLOCK_LIMIT_NUM) {
      BlockList reply = wallet.getBlocksByLimitNext(startNum, endNum - startNum);
      if (reply != null) {
        response.getWriter().println(Util.printBlockList(reply, visible));
        return;
      }
    }
    response.getWriter().println("{}");
  }
}