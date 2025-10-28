package com.dzarembo.hyperliquidservice.clinet;

import com.dzarembo.hyperliquidservice.model.FundingRate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class HyperliquidApiClient {

    private final WebClient webClient = WebClient.create("https://api.hyperliquid.xyz");

    /**
     * Загружает актуальные funding rates из Hyperliquid (предсказанные на ближайший час).
     * Время nextFundingTime возвращается в UTC, что соответствует локальному времени Минска (UTC+3)
     * при отображении на сайте и в анализаторе.
     */
    public List<FundingRate> fetchFundingRates() {
        try {
            log.info("🔄 Fetching funding rates from Hyperliquid (predictedFundings)...");

            Map<String, Object> body = Map.of("type", "predictedFundings");

            Object[] response = webClient.post()
                    .uri("/info")
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Object[].class)
                    .onErrorResume(WebClientResponseException.class, e -> {
                        log.error("❌ Hyperliquid API error: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response == null) {
                log.warn("⚠️ Empty response from Hyperliquid predictedFundings");
                return List.of();
            }

            List<FundingRate> result = new ArrayList<>();

            for (Object coinObj : response) {
                if (!(coinObj instanceof List<?> coinBlock) || coinBlock.size() < 2)
                    continue;

                String symbol = (String) coinBlock.get(0);
                List<?> exchanges = (List<?>) coinBlock.get(1);

                for (Object ex : exchanges) {
                    if (!(ex instanceof List<?> exBlock) || exBlock.size() < 2)
                        continue;

                    String exchangeName = (String) exBlock.get(0);
                    Map<String, Object> data = (Map<String, Object>) exBlock.get(1);

                    // Берём только рынок Hyperliquid Perpetual
                    if (!"HlPerp".equals(exchangeName))
                        continue;

                    try {
                        double rate = Double.parseDouble(data.get("fundingRate").toString());

                        // 🔹 Фильтрация: если funding = 0, пропускаем пару. Потому что такие пары на самой бирже не представлены
                        //только по api они приходят
                        if (rate == 0.0d) {
                            log.debug("⏭️ Skipping {} (inactive pair, funding=0)", symbol);
                            continue;
                        }

                        int interval = ((Number) data.getOrDefault("fundingIntervalHours", 1)).intValue();
                        long nextFundingTime = ((Number) data.get("nextFundingTime")).longValue();

                        //добавляем один час так как время в utc почему-то приходит текущее т.е. если сейчас 12 30 то приходит время
                        //время 12 00 (9ютс или 12ютс+3) а не 13 00, так как фандинг начисляется раз в час то просто добавим вручную час
                        // и тогда в анализаторе все будет отображаться как и у других бирж
                        nextFundingTime += 3600_000L;

                        FundingRate fundingRate = new FundingRate(
                                normalizeSymbol(symbol),
                                rate,              // это уже почасовой rate, делить не нужно
                                nextFundingTime,   // API отдаёт UTC, при отображении будет локальное время
                                interval           // почти всегда 1
                        );

                        result.add(fundingRate);

                    } catch (Exception parseEx) {
                        log.warn("⚠️ Failed to parse HL item for {}: {}", symbol, parseEx.getMessage());
                    }
                }
            }

            log.info("✅ Parsed {} hourly funding pairs from Hyperliquid", result.size());
            return result;

        } catch (Exception e) {
            log.error("💥 Failed to fetch Hyperliquid funding rates", e);
            return List.of();
        }
    }

    /**
     * Приводим имя символа к виду BTCUSDT, MEGAUSDT и т.п.
     */
    private String normalizeSymbol(String name) {
        name = name.toUpperCase();
        if (!name.endsWith("USDT") && !name.endsWith("USD") && !name.endsWith("USDC")) {
            name += "USDT";
        }
        return name;
    }
}