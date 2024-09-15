package com.tencent.wxcloudrun.core.usecase;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.core.usecase.listner.StockListener;
import com.tencent.wxcloudrun.dto.StockInfo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StockMonitorCoordinator {
    public ApiResponse getStockInfo() {
        List<StockInfo> stockInfoList = StockListener.stockInfoList;
        return ApiResponse.ok(stockInfoList);
    }
}
