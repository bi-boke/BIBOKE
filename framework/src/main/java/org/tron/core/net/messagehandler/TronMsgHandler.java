package org.bok.core.net.messagehandler;

import org.bok.core.exception.P2pException;
import org.bok.core.net.message.UnMessage;
import org.bok.core.net.peer.PeerConnection;

public interface UnMsgHandler {

  void processMessage(PeerConnection peer, UnMessage msg) throws P2pException;

}
