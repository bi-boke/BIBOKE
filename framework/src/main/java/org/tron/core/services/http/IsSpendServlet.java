package org.bok.core.services.http;

import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bok.api.GrpcAPI.NoteParameters;
import org.bok.api.GrpcAPI.SpendResult;
import org.bok.core.Wallet;


@Component
@Slf4j(topic = "API")
public class IsSpendServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {

  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      boolean visible = Util.getVisiblePost(input);

      NoteParameters.Builder build = NoteParameters.newBuilder();
      JsonFormat.merge(input, build, visible);

      SpendResult result = wallet.isSpend(build.build());
      response.getWriter().println(JsonFormat.printToString(result, visible));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
