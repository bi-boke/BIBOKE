package org.bok.core.services.http;

import static org.bok.core.db.TransactionTrace.convertToUnAddress;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bok.api.GrpcAPI.NumberMessage;
import org.bok.api.GrpcAPI.TransactionInfoList;
import org.bok.core.Wallet;
import org.bok.protos.Protocol.TransactionInfo;
import org.bok.protos.Protocol.TransactionInfo.Log;

@Component
@Slf4j(topic = "API")
public class GetTransactionInfoByBlockNumServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  private JSONObject convertLogAddressToUnAddress(TransactionInfo transactionInfo,
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
          logBuilder.setAddress(ByteString.copyFrom(convertToUnAddress(newAddress)));
        }
        newLogList.add(logBuilder.build());
      }
      transactionInfo = transactionInfo.toBuilder().clearLog().addAllLog(newLogList).build();
    }

    return JSONObject.parseObject(JsonFormat.printToString(transactionInfo, visible));
  }

  private String printTransactionInfoList(TransactionInfoList list, boolean selfType) {
    JSONArray jsonArray = new JSONArray();
    for (TransactionInfo transactionInfo : list.getTransactionInfoList()) {
      jsonArray.add(convertLogAddressToUnAddress(transactionInfo, selfType));
    }
    return jsonArray.toJSONString();
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      long num = Long.parseLong(request.getParameter("num"));

      if (num > 0L) {
        TransactionInfoList reply = wallet.getTransactionInfoByBlockNum(num);
        response.getWriter().println(printTransactionInfoList(reply, visible));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      NumberMessage.Builder build = NumberMessage.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());

      long num = build.getNum();
      if (num > 0L) {
        TransactionInfoList reply = wallet.getTransactionInfoByBlockNum(num);
        response.getWriter().println(printTransactionInfoList(reply, params.isVisible()));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }
}
