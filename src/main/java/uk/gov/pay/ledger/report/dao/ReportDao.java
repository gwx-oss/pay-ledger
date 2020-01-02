package uk.gov.pay.ledger.report.dao;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import uk.gov.pay.ledger.report.entity.PaymentCountByStateResult;
import uk.gov.pay.ledger.report.entity.TimeseriesReportSlice;
import uk.gov.pay.ledger.report.entity.TransactionsStatisticsResult;
import uk.gov.pay.ledger.report.mapper.ReportMapper;
import uk.gov.pay.ledger.report.params.TransactionSummaryParams;
import uk.gov.pay.ledger.transaction.model.TransactionType;
import uk.gov.pay.ledger.transaction.state.TransactionState;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;

@RegisterRowMapper(ReportMapper.class)
public class ReportDao {
    private static final String COUNT_TRANSACTIONS_BY_STATE = "SELECT state, count(1) AS count FROM transaction t " +
            "WHERE type = :transactionType::transaction_type " +
            ":searchExtraFields " +
            "GROUP BY state";

    private static final String TRANSACTION_SUMMARY_STATISTICS = "SELECT count(1) AS count, " +
            "SUM(CASE WHEN total_amount IS NULL THEN amount ELSE total_amount END) AS grossAmount " +
            "FROM transaction t " +
            "WHERE type = :transactionType::transaction_type " +
            "AND state = :state " +
            ":searchExtraFields ";

    private final Jdbi jdbi;

    @Inject
    public ReportDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<PaymentCountByStateResult> getPaymentCountsByState(TransactionSummaryParams params) {
        return jdbi.withHandle(handle -> {
            String template = createSearchTemplate(params.getFilterTemplates(),
                    COUNT_TRANSACTIONS_BY_STATE);

            Query query = handle.createQuery(template)
                    .bind("transactionType", TransactionType.PAYMENT);
            params.getQueryMap().forEach(query::bind);

            return query.map((rs, rowNum) -> {
                String state = rs.getString("state");
                Long count = rs.getLong("count");
                return new PaymentCountByStateResult(state, count);
            }).list();
        });
    }

    public TransactionsStatisticsResult getTransactionSummaryStatistics(TransactionSummaryParams params, TransactionType transactionType) {
        return jdbi.withHandle(handle -> {
            String template = createSearchTemplate(params.getFilterTemplates(), TRANSACTION_SUMMARY_STATISTICS);

            Query query = handle.createQuery(template)
                    .bind("transactionType", transactionType)
                    .bind("state", TransactionState.SUCCESS);
            params.getQueryMap().forEach(query::bind);

            return query.map((rs, rowNum) -> {
                long count = rs.getLong("count");
                long grossAmount = rs.getLong("grossAmount");
                return new TransactionsStatisticsResult(count, grossAmount);
            }).findOnly();
        });
    }

    // @TODO(sfount) using an interface with annotations seems like a neater way of writing a DAO (see events)
    // @TODO(sfount) selecting this report by individual gateway account would be useful in the future
    public List<TimeseriesReportSlice> getTransactionsVolumeByTimeseries(ZonedDateTime fromDate, ZonedDateTime toDate) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT " +
                    "date_trunc('hour', t.created_date) as timestamp, " +
                    "COUNT(1) as all_payments, " +
                    "COUNT(1) filter (WHERE t.state IN ('ERROR', 'GATEWAY_ERROR')) as errored_payments, " +
                    "COUNT(1) filter (WHERE t.state IN ('SUCCESS')) as completed_payments, " +
                    "SUM(t.amount) as amount, SUM(t.net_amount) as net_amount, SUM(t.total_amount) as total_amount, SUM(t.fee) as fee " +
                    "FROM transaction t " +
                    "WHERE t.live AND t.created_date >= :fromDate AND t.created_date <= :toDate " +
                    "GROUP BY date_trunc('hour', t.created_date) " +
                    "ORDER BY date_trunc('hour', t.created_date)")
                    .bind("fromDate", fromDate)
                    .bind("toDate", toDate)
                    .map(new ReportMapper())
                    .list()
        );
    }

    private String createSearchTemplate(
            List<String> filterTemplates,
            String baseQueryString) {

        String searchClauseTemplate = String.join(" AND ", filterTemplates);
        searchClauseTemplate = StringUtils.isNotBlank(searchClauseTemplate) ?
                "AND " + searchClauseTemplate :
                "";

        return baseQueryString.replace(
                ":searchExtraFields",
                searchClauseTemplate);
    }
}
