package com.tencent.wxcloudrun.core.usecase.listner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.dto.Content;
import com.tencent.wxcloudrun.dto.Feishu;
import com.tencent.wxcloudrun.dto.StockInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StockListener implements ApplicationListener<ContextRefreshedEvent> {
    public static List<StockInfo> stockInfoList = new LinkedList<>();
    private final RestTemplate restTemplate;

    public StockListener(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // iphone
        String url = "https://www.apple.com.cn/shop/pickup-message-recommendations?mts.0=regular&mts.1=compact&cppart=UNLOCKED/WW&searchNearby=true&store=R683&product=MYTM3CH/A";
        // watch
        // String url = "https://www.apple.com.cn/shop/fulfillment-messages?searchNearby=true&parts.0=MQFG3CH/A&option.0=MQG03CH/A,MQEP3FE/A&store=R581";
        String webhooks = "https://open.feishu.cn/open-apis/bot/v2/hook/269714ca-790b-4f6e-8c4e-0eb8dcad85a1";

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] preBytes = new byte[0];
        while (true) {
            try {

                ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
                if (!exchange.getStatusCode().is2xxSuccessful()) {
                    continue;
                }
                String body = exchange.getBody();
                log.info("[{}]{}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), body);
                Map<String, Object> recommend = new HashMap<>();

                recommend = objectMapper.readValue(body, recommend.getClass());

                List<StockInfo> stockInfos = ((List<Map<String, Object>>) ((Map<String, Object>) ((Map<String, Object>) recommend.get("body")).get("PickupMessage")).get("stores"))
                        .stream()
                        .filter(x -> !CollectionUtils.isEmpty(x))
                        .map(x -> (Map<String, Object>) x.get("partsAvailability"))
                        .filter(x -> !CollectionUtils.isEmpty(x))
                        .map(Map::values)
                        .flatMap(x -> x.stream().map(z -> (Map<String, Object>) z)
                                .filter(z -> {
                                    Object partNumber = z.get("partNumber");
                                    return Objects.nonNull(partNumber) && partNumber.toString().startsWith("MYT");
                                })
                                .map(y -> (Map<String, Object>) ((Map<String, Object>) y.get("messageTypes")).get("regular"))
                        )
                        .map(x -> {
                            StockInfo stockInfo = new StockInfo();
                            stockInfo.setStock((String) x.get("storePickupProductTitle"));
                            String storePickupQuote = (String) x.get("storePickupQuote");
                            if (storePickupQuote != null) {
                                String[] split = storePickupQuote.split("；");
                                if (split.length == 2) {
                                    stockInfo.setPlace(split[1]);
                                    stockInfo.setPickTime(split[0]);
                                }
                            }
                            return stockInfo;
                        }).collect(Collectors.toList());
                byte[] bytes = objectMapper.writeValueAsBytes(stockInfos);
                if (Arrays.equals(preBytes, bytes)) {
                    continue;
                }
                preBytes = bytes;
                stockInfoList = stockInfos;
                if (!CollectionUtils.isEmpty(stockInfoList)) {
                    System.out.println("======找到啦" + stockInfoList + "找到啦======");
                    Feishu content = new Feishu("text", new Content(stockInfoList.toString()));
                    HttpEntity<Feishu> entity = new HttpEntity<>(content);
                    exchange = restTemplate.exchange(webhooks, HttpMethod.POST, entity, String.class);
                    if (!exchange.getStatusCode().is2xxSuccessful()) {
                        continue;
                    }
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                try {
                    Thread.sleep(2000 + new Random().nextInt(1000));
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }
}
