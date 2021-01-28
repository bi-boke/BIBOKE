package org.bok.core.net.messagehandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bok.core.config.args.Args;
import org.bok.core.exception.P2pException;
import org.bok.core.exception.P2pException.TypeEnum;
import org.bok.core.net.UnNetDelegate;
import org.bok.core.net.message.TransactionMessage;
import org.bok.core.net.message.TransactionsMessage;
import org.bok.core.net.message.UnMessage;
import org.bok.core.net.peer.Item;
import org.bok.core.net.peer.PeerConnection;
import org.bok.core.net.service.AdvService;
import org.bok.protos.Protocol.Inventory.InventoryType;
import org.bok.protos.Protocol.ReasonCode;
import org.bok.protos.Protocol.Transaction;
import org.bok.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j(topic = "net")
@Component
public class TransactionsMsgHandler implements UnMsgHandler {

  private static int MAX_UN_SIZE = 50_000;
  private static int MAX_SMART_CONTRACT_SUBMIT_SIZE = 100;
  @Autowired
  private UnNetDelegate unNetDelegate;
  @Autowired
  private AdvService advService;

  //  private static int TIME_OUT = 10 * 60 * 1000;
  private BlockingQueue<UnEvent> smartContractQueue = new LinkedBlockingQueue(MAX_UN_SIZE);

  private BlockingQueue<Runnable> queue = new LinkedBlockingQueue();

  private int threadNum = Args.getInstance().getValidateSignThreadNum();
  private ExecutorService trxHandlePool = new ThreadPoolExecutor(threadNum, threadNum, 0L,
      TimeUnit.MILLISECONDS, queue);

  private ScheduledExecutorService smartContractExecutor = Executors
      .newSingleThreadScheduledExecutor();

  public void init() {
    handleSmartContract();
  }

  public void close() {
    smartContractExecutor.shutdown();
  }

  public boolean isBusy() {
    return queue.size() + smartContractQueue.size() > MAX_UN_SIZE;
  }

  @Override
  public void processMessage(PeerConnection peer, UnMessage msg) throws P2pException {
    TransactionsMessage transactionsMessage = (TransactionsMessage) msg;
    check(peer, transactionsMessage);
    for (Transaction trx : transactionsMessage.getTransactions().getTransactionsList()) {
      int type = trx.getRawData().getContract(0).getType().getNumber();
      if (type == ContractType.TriggerSmartContract_VALUE
          || type == ContractType.CreateSmartContract_VALUE) {
        if (!smartContractQueue.offer(new UnEvent(peer, new TransactionMessage(trx)))) {
          logger.warn("Add smart contract failed, queueSize {}:{}", smartContractQueue.size(),
              queue.size());
        }
      } else {
        trxHandlePool.submit(() -> handleTransaction(peer, new TransactionMessage(trx)));
      }
    }
  }

  private void check(PeerConnection peer, TransactionsMessage msg) throws P2pException {
    for (Transaction trx : msg.getTransactions().getTransactionsList()) {
      Item item = new Item(new TransactionMessage(trx).getMessageId(), InventoryType.UN);
      if (!peer.getAdvInvRequest().containsKey(item)) {
        throw new P2pException(TypeEnum.BAD_MESSAGE,
            "trx: " + msg.getMessageId() + " without request.");
      }
      peer.getAdvInvRequest().remove(item);
    }
  }

  private void handleSmartContract() {
    smartContractExecutor.scheduleWithFixedDelay(() -> {
      try {
        while (queue.size() < MAX_SMART_CONTRACT_SUBMIT_SIZE) {
          UnEvent event = smartContractQueue.take();
          trxHandlePool.submit(() -> handleTransaction(event.getPeer(), event.getMsg()));
        }
      } catch (Exception e) {
        logger.error("Handle smart contract exception.", e);
      }
    }, 1000, 20, TimeUnit.MILLISECONDS);
  }

  private void handleTransaction(PeerConnection peer, TransactionMessage trx) {
    if (peer.isDisconnect()) {
      logger.warn("Drop trx {} from {}, peer is disconnect.", trx.getMessageId(),
          peer.getInetAddress());
      return;
    }

    if (advService.getMessage(new Item(trx.getMessageId(), InventoryType.UN)) != null) {
      return;
    }

    try {
      unNetDelegate.pushTransaction(trx.getTransactionCapsule());
      advService.broadcast(trx);
    } catch (P2pException e) {
      logger.warn("Un {} from peer {} process failed. type: {}, reason: {}",
          trx.getMessageId(), peer.getInetAddress(), e.getType(), e.getMessage());
      if (e.getType().equals(TypeEnum.BAD_UN)) {
        peer.disconnect(ReasonCode.BAD_TX);
      }
    } catch (Exception e) {
      logger.error("Un {} from peer {} process failed.", trx.getMessageId(), peer.getInetAddress(),
          e);
    }
  }

  class UnEvent {

    @Getter
    private PeerConnection peer;
    @Getter
    private TransactionMessage msg;
    @Getter
    private long time;

    public UnEvent(PeerConnection peer, TransactionMessage msg) {
      this.peer = peer;
      this.msg = msg;
      this.time = System.currentTimeMillis();
    }
  }
}