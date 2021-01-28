package org.bok.common.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.bok.common.overlay.discover.DiscoverServer;
import org.bok.common.overlay.discover.node.NodeManager;
import org.bok.common.overlay.server.ChannelManager;
import org.bok.core.db.Manager;

public class UnApplicationContext extends AnnotationConfigApplicationContext {

  public UnApplicationContext() {
  }

  public UnApplicationContext(DefaultListableBeanFactory beanFactory) {
    super(beanFactory);
  }

  public UnApplicationContext(Class<?>... annotatedClasses) {
    super(annotatedClasses);
  }

  public UnApplicationContext(String... basePackages) {
    super(basePackages);
  }

  @Override
  public void destroy() {

    Application appT = ApplicationFactory.create(this);
    appT.shutdownServices();
    appT.shutdown();

    DiscoverServer discoverServer = getBean(DiscoverServer.class);
    discoverServer.close();
    ChannelManager channelManager = getBean(ChannelManager.class);
    channelManager.close();
    NodeManager nodeManager = getBean(NodeManager.class);
    nodeManager.close();

    Manager dbManager = getBean(Manager.class);
    dbManager.stopRePushThread();
    dbManager.stopRePushTriggerThread();
    super.destroy();
  }
}
