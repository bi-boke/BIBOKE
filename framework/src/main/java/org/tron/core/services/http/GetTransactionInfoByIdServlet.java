package org.bok.core.services.http;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bok.api.GrpcAPI.BytesMessage;
import org.bok.common.utils.ByteArray;
import org.bok.core.Wallet;
import org.bok.core.db.TransactionTrace;
import org.bok.core.utils.TransactionUtil;
import org.bok.core.vm.utils.MUtil;
import org.bok.protos.Protocol.TransactionInfo;
import org.bok.protos.Protocol.TransactionInfo.Log;


@Component
@Slf4j(topic = "API")
public class GetTransactionInfoByIdServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  private static String convertLogAddressToUnAddress(TransactionInfo transactionInfo,
      boolean visible) {
    if (visible) {
      List<Log> newLogList = new ArrayList<>();
      for (Log log : transactionInfo.getLogList()) {
        Log.Builder logBuilder = Log.newBuilder();
        logBuilder.setData(log.getData());
        logBuilder.addAllTopics(log.getTopicsList());

        byte[] oldAddress = log.getAddress().toByteArray();
        if (oldAddress.length == 0 || oldAddress.length > 20) {
          logBuilder.setAddress(log.getAddress());
        } else {
          byte[] newAddress = new byte[20];

          int start = 20 - oldAddress.length;
          System.arraycopy(oldAddress, 0, newAddress, start, oldAddress.length);
          logBuilder.setAddress(ByteString.copyFrom(TransactionTrace
              .convertToUnAddress(newAddress)));
        }
        newLogList.add(logBuilder.build());
      }
      transactionInfo = transactionInfo.toBuilder().clearLog().addAllLog(newLogList).build();
    }
    return JsonFormat.printToString(transactionInfo, visible);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String input = request.getParameter("value");
      TransactionInfo reply = wallet
          .getTransactionInfoById(ByteString.copyFrom(ByteArray.fromHexString(input)));
      if (reply != null) {
        response.getWriter().println(convertLogAddressToUnAddress(reply, visible));
      } else {
        response.getWriter().println("{}");
      }
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
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      TransactionInfo reply = wallet.getTransactionInfoById(build.getValue());
      if (reply != null) {
        response.getWriter().println(convertLogAddressToUnAddress(reply, visible));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}