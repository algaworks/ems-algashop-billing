package com.algaworks.algashop.billing.infrastructure.persistence.metrics;

import com.algaworks.algashop.billing.application.metrics.MetricsQueryService;
import com.algaworks.algashop.billing.application.metrics.PaymentsOverviewOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MetricsQueryServiceImpl implements MetricsQueryService {

    private static final String PAYMENTS_OVERVIEW_SQL = """
            select to_char(date_trunc('month', i.paid_at), 'yyyy-mm') as year_month,
                   ps.method as payment_method,
                   count(i.id) as quantity,
                   coalesce(sum(i.total_amount), 0) as total_amount
            from invoice i
            inner join payment_settings ps on ps.id = i.payment_settings_id
            where i.status = 'PAID'
                and i.paid_at >= cast(:startDate as date)
                and i.paid_at < cast(:endDateExclusive as date)
            group by 1, 2
            order by 1, 2
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<PaymentsOverviewOutput> getPaymentsOverview(YearMonth yearMonthStart, YearMonth yearMonthEnd) {
        if (yearMonthStart == null) {
            yearMonthStart = Year.now().atMonth(1);
        }
        if (yearMonthEnd == null) {
            yearMonthEnd = Year.now().atMonth(12);
        }
        LocalDate startDate = yearMonthStart.atDay(1);
        LocalDate endDateExclusive = yearMonthEnd.atEndOfMonth().plusDays(1);

        return jdbcTemplate.query(
                PAYMENTS_OVERVIEW_SQL,
                Map.of("startDate", startDate, "endDateExclusive", endDateExclusive),
                (rs, rowNum) -> new PaymentsOverviewOutput(
                        YearMonth.parse(rs.getString("year_month")),
                        rs.getString("payment_method"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("total_amount")));
    }
}
