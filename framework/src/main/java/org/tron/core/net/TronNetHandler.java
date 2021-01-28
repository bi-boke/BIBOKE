package org.bok.core.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.bok.common.overlay.server.Channel;
import org.bok.common.overlay.server.MessageQueue;
import org.bok.core.net.message.UnMessage;
import org.bok.core.net.peer.PeerConnection;

@Component
@Scope("prototype")
public class UnNetHandler extends SimpleChannelInboundHandler<UnMessage> {

  protected PeerConnection peer;

  private MessageQueue msgQueue;

  @Autowired
  private UnNetService unNetService;

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, UnMessage msg) throws Exception {
    msgQueue.receivedMessage(msg);
    unNetService.onMessage(peer, msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    peer.processException(cause);
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }

  public void setChannel(Channel channel) {
    this.peer = (PeerConnection) channel;
  }

}