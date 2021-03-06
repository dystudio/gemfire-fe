package io.pivotal.bds.gemfire.bitcoinj.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.geode.cache.RegionEvent;
import org.apache.geode.cache.util.CacheListenerAdapter;

public class SerializedWalletCacheListener extends CacheListenerAdapter<String, byte[]> {

    private static final Logger LOG = LoggerFactory.getLogger(SerializedWalletCacheListener.class);

    @Override
    public void afterRegionLive(RegionEvent<String, byte[]> event) {
        LOG.info("afterRegionLive: event={}", event);
    }

}
