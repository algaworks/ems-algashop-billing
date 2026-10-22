package com.algaworks.algashop.billing.application.metrics;

import java.time.YearMonth;
import java.util.List;

public interface MetricsQueryService {
    List<PaymentsOverviewOutput> getPaymentsOverview(YearMonth yearMonthStart, YearMonth yearMonthEnd);
}
