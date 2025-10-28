package com.dzarembo.hyperliquidservice.controller;

import com.dzarembo.hyperliquidservice.cache.FundingCache;
import com.dzarembo.hyperliquidservice.model.FundingRate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/hyperliquid")
@RequiredArgsConstructor
public class HyperliquidFundingController {

    private final FundingCache cache;

    @GetMapping("/funding")
    public Collection<FundingRate> getFundingRates() {
        return cache.getAll();
    }
}
