package io.pivotal.bds.gemfire.xrefs.server.listener;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.partition.PartitionListener;
import com.gemstone.gemfire.cache.partition.PartitionRegionHelper;

import io.pivotal.bds.gemfire.xrefs.common.SecurityKey;
import io.pivotal.bds.gemfire.xrefs.common.SecurityPriceHistory;
import io.pivotal.bds.gemfire.xrefs.common.SecurityPriceHistoryKey;
import io.pivotal.bds.gemfire.xrefs.server.data.PDXConcurrentList;

public class SecurityPriceHistoryXrefPartitionListener implements PartitionListener {

    private Region<SecurityPriceHistoryKey, SecurityPriceHistory> securityPriceHistoryRegion;
    private Region<SecurityKey, PDXConcurrentList<SecurityPriceHistoryKey>> securityPriceXrefRegion;

    private static final Logger LOG = LoggerFactory.getLogger(SecurityPriceHistoryXrefPartitionListener.class);

    public SecurityPriceHistoryXrefPartitionListener(
            Region<SecurityKey, PDXConcurrentList<SecurityPriceHistoryKey>> securityPriceXrefRegion) {
        this.securityPriceXrefRegion = securityPriceXrefRegion;
    }

    @Override
    public void afterBucketCreated(int bucketId, Iterable<?> keys) {
        LOG.info("afterBucketCreated: bucketId={}", bucketId);
    }

    @Override
    public void afterBucketRemoved(int bucketId, Iterable<?> keys) {
        LOG.info("afterBucketRemoved: bucketId={}", bucketId);
        
        for (Object ok: keys) {
            SecurityPriceHistoryKey sphk = (SecurityPriceHistoryKey)ok;
            SecurityPriceHistory sph = securityPriceHistoryRegion.get(sphk);
            SecurityKey sk = sph.getSecurityKey();

            PDXConcurrentList<SecurityPriceHistoryKey> l = securityPriceXrefRegion.get(sk);
            l.remove(sphk);
            securityPriceXrefRegion.put(sk, l);
        }
    }

    @Override
    public void afterPrimary(int bucketId) {
        LOG.info("afterPrimary: bucketId={}", bucketId);
        Region<SecurityPriceHistoryKey, SecurityPriceHistory> r = PartitionRegionHelper.getLocalData(securityPriceHistoryRegion);
        
        for (Map.Entry<SecurityPriceHistoryKey, SecurityPriceHistory> entry : r.entrySet()) {
            SecurityPriceHistoryKey sphk = entry.getKey();
            SecurityPriceHistory sph = entry.getValue();
            SecurityKey sk = sph.getSecurityKey();

            PDXConcurrentList<SecurityPriceHistoryKey> l = securityPriceXrefRegion.get(sk);
            l.add(sphk);
            securityPriceXrefRegion.put(sk, l);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void afterRegionCreate(Region<?, ?> region) {
        LOG.info("afterRegionCreate: region={}", region.getName());
        securityPriceHistoryRegion = (Region<SecurityPriceHistoryKey, SecurityPriceHistory>) region;
    }

}
