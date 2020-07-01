package uk.gov.pay.ledger.transaction.search.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.pay.ledger.exception.UnparsableDateException;
import uk.gov.pay.ledger.exception.ValidationException;
import uk.gov.pay.ledger.util.CommaDelimitedSetParameter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransactionSearchParamsValidatorTest {

    TransactionSearchParams searchParams;

    @BeforeEach
    public void setup() {
        searchParams = new TransactionSearchParams();
    }

    @Test
    public void shouldThrowException_whenInvalidFromDate() {
        searchParams.setFromDate("wrong-date");
        UnparsableDateException unparsableDateException = assertThrows(UnparsableDateException.class,
                () -> TransactionSearchParamsValidator.validateSearchParams(searchParams, null));
        assertThat(unparsableDateException.getMessage(), is("Input from_date (wrong-date) is wrong format"));
    }

    @Test
    public void shouldThrowException_whenInvalidToDate() {
        searchParams.setToDate("wrong-date");
        UnparsableDateException unparsableDateException = assertThrows(UnparsableDateException.class,
                () -> TransactionSearchParamsValidator.validateSearchParams(searchParams, null));
        assertThat(unparsableDateException.getMessage(), is("Input to_date (wrong-date) is wrong format"));
    }

    @Test
    public void shouldNotThrowException_whenValidDateFormats() {
        searchParams.setFromDate("2019-05-01T10:15:30Z");
        searchParams.setToDate("2019-05-01T10:15:30Z");
        TransactionSearchParamsValidator.validateSearchParams(searchParams, null);
    }

    @Test
    public void shouldNotThrowException_whenGatewayAccountIdAvailableAndWithParentTransactionIsTrue() {
        searchParams.setWithParentTransaction(true);
        TransactionSearchParamsValidator.validateSearchParams(searchParams, new CommaDelimitedSetParameter("account-id"));
    }

    @Test
    public void shouldNotThrowException_whenGatewayAccountIdsAvailableAndWithParentTransactionIsTrue() {
        searchParams.setWithParentTransaction(true);
        ValidationException validationException = assertThrows(ValidationException.class,
                () -> TransactionSearchParamsValidator.validateSearchParams(searchParams, new CommaDelimitedSetParameter("")));
        assertThat(validationException.getMessage(), is("gateway_account_id is mandatory to search with parent transaction"));
    }

    @Test
    public void shouldNotThrowException_whenGatewayAccountIdIsNotAvailableAndWithParentTransactionIsTrue() {
        searchParams.setWithParentTransaction(true);
        TransactionSearchParamsValidator.validateSearchParams(searchParams, new CommaDelimitedSetParameter("1,2"));
    }

    @Test
    public void validateSearchParamsForCsvShouldNotThrowExceptionForValidParams() {
        searchParams.setFromDate("2019-05-01T10:15:30Z");
        searchParams.setToDate("2019-05-01T10:15:30Z");
        TransactionSearchParamsValidator.validateSearchParamsForCsv(
                searchParams, new CommaDelimitedSetParameter("1,2"));
    }

    @Test
    public void validateSearchParamsForCsvShouldThrowExceptionIfGatewayAccountIsNotAvailable() {
        assertThrows(ValidationException.class, () -> TransactionSearchParamsValidator.validateSearchParamsForCsv(
                searchParams, null));
    }

    @Test
    public void validateSearchParamsForCsvShouldThrowExceptionIfGatewayAccountIdsIsEmptyString() {
        assertThrows(ValidationException.class, () -> TransactionSearchParamsValidator.validateSearchParamsForCsv(
                searchParams, new CommaDelimitedSetParameter("")));
    }

    @ParameterizedTest
    @CsvSource({
            "wrong-date, 2019-05-01T10:15:30Z",
            "2019-05-01T10:15:30Z, wrong-date"
    })
    public void validateSearchParamsForCsvShouldThrowExceptionForInvalidDates(String fromDate, String toDate) {
        searchParams.setFromDate(fromDate);
        searchParams.setToDate(toDate);

        assertThrows(UnparsableDateException.class, () -> TransactionSearchParamsValidator.validateSearchParamsForCsv(
                searchParams, new CommaDelimitedSetParameter("1")));
    }
}