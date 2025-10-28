package com.dzarembo.hyperliquidservice.update;

import com.dzarembo.hyperliquidservice.cache.FundingCache;
import com.dzarembo.hyperliquidservice.clinet.HyperliquidApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class HyperliquidFundingUpdater {
    private final FundingCache cache;
    private final HyperliquidApiClient apiClient;

    @Scheduled(fixedRate = 1 * 60 * 1000)
    public void updateFundingRates() {
        log.info("Updating funding cache...");
        cache.putAll(apiClient.fetchFundingRates());
        log.info("Hyperliquid funding cache updated: {} entries", cache.getAll().size());
    }
}
