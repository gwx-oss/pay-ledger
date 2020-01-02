package uk.gov.pay.ledger.transaction.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.ledger.transaction.entity.TransactionEntity;
import uk.gov.pay.ledger.transaction.state.TransactionState;
import uk.gov.pay.ledger.util.fixture.TransactionFixture;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.ledger.util.fixture.TransactionFixture.aTransactionFixture;

public class CsvTransactionFactoryTest {

    private CsvTransactionFactory csvTransactionFactory;
    private TransactionFixture transactionFixture;

    @Before
    public void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        csvTransactionFactory = new CsvTransactionFactory(objectMapper);

        transactionFixture = aTransactionFixture()
                .withState(TransactionState.FAILED_REJECTED)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withAmount(100L)
                .withCreatedDate(ZonedDateTime.parse("2018-03-12T16:25:01.123456Z"))
                .withTotalAmount(123L)
                .withTransactionDetails(
                        new GsonBuilder().create()
                                .toJson(ImmutableMap.builder()
                                        .put("expiry_date", "10/24")
                                        .put("user_email", "refundedbyuser@example.org")
                                        .put("corporate_surcharge", 23)
                                        .put("wallet", "APPLE_PAY")
                                        .put("card_type", "DEBIT")
                                        .put("card_brand_label", "Visa")
                                        .build())
                );
    }

    @Test
    public void toMapShouldReturnMapWithCorrectCsvDataForPaymentTransaction() {

        TransactionEntity transactionEntity = transactionFixture.toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertPaymentDetails(csvDataMap, transactionEntity);

        assertThat(csvDataMap.get("Amount"), is("1.00"));
        assertThat(csvDataMap.get("State"), is("Declined"));
        assertThat(csvDataMap.get("Finished"), is(true));
        assertThat(csvDataMap.get("Error Code"), is("P0010"));
        assertThat(csvDataMap.get("Error Message"), is("Payment method rejected"));
        assertThat(csvDataMap.get("Date Created"), is("12 Mar 2018"));
        assertThat(csvDataMap.get("Time Created"), is("16:25:01"));
        assertThat(csvDataMap.get("Corporate Card Surcharge"), is("0.23"));
        assertThat(csvDataMap.get("Total Amount"), is("1.23"));
    }

    @Test
    public void toMapShouldReturnMapWithCorrectCsvDataForRefundTransaction() {

        TransactionEntity transactionEntity = transactionFixture.toEntity();
        TransactionEntity refundTransactionEntity = transactionFixture.withTransactionType("REFUND")
                .withAmount(99L)
                .withTotalAmount(99L)
                .withState(TransactionState.ERROR)
                .withParentTransactionEntity(transactionEntity)
                .withTransactionDetails(new GsonBuilder().create()
                        .toJson(ImmutableMap.builder().put("user_email", "refundedbyuser@example.org").build()))
                .toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(refundTransactionEntity);

        assertPaymentDetails(csvDataMap, refundTransactionEntity.getParentTransactionEntity());
        assertThat(csvDataMap.get("Amount"), is("-0.99"));
        assertThat(csvDataMap.get("State"), is("Refund error"));
        assertThat(csvDataMap.get("Finished"), is(true));
        assertThat(csvDataMap.get("Error Code"), is("P0050"));
        assertThat(csvDataMap.get("Error Message"), is("Payment provider returned an error"));
        assertThat(csvDataMap.get("Date Created"), is("12 Mar 2018"));
        assertThat(csvDataMap.get("Time Created"), is("16:25:01"));
        assertThat(csvDataMap.get("Corporate Card Surcharge"), is("0.00"));
        assertThat(csvDataMap.get("Total Amount"), is("-0.99"));
        assertThat(csvDataMap.get("Net"), is("-0.99"));
    }

    @Test
    public void toMapShouldIncludeExternalMetadataFields() {
        TransactionEntity transactionEntity = transactionFixture.withTransactionDetails(
                new GsonBuilder().create().toJson(ImmutableMap.builder()
                        .put("external_metadata",
                                ImmutableMap.builder()
                                        .put("key-1", "value-1").put("key-2", "value-2").build())
                        .build()
                )).toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertThat(csvDataMap.get("key-1 (metadata)"), is("value-1"));
        assertThat(csvDataMap.get("key-2 (metadata)"), is("value-2"));
    }

    @Test
    public void toMapShouldIncludeFeeAndNetAmountForStripePayments() {
        TransactionEntity transactionEntity = transactionFixture.withNetAmount(594)
                .withPaymentProvider("stripe")
                .withTransactionType(TransactionType.PAYMENT.name())
                .withFee(6).toEntity();

        Map<String, Object> csvDataMap = csvTransactionFactory.toMap(transactionEntity);

        assertThat(csvDataMap.get("Net"), is("5.94"));
        assertThat(csvDataMap.get("Fee"), is("0.06"));
    }

    @Test
    public void getCsvHeadersShouldReturnMapWithCorrectCsvHeaders() {
        TransactionEntity transactionEntity = transactionFixture.withNetAmount(594)
                .withState(TransactionState.FAILED_REJECTED)
                .withTransactionType(TransactionType.PAYMENT.name())
                .withPaymentProvider("sandbox")
                .withFee(6).toEntity();

        List transactionEntities = List.of(transactionEntity);

        LinkedHashMap<String, Object> csvHeaders = (LinkedHashMap<String, Object>) csvTransactionFactory.getCsvHeaders(transactionEntities);

        assertThat(csvHeaders.get("Reference"), is(notNullValue()));
        assertThat(csvHeaders.get("Description"), is(notNullValue()));
        assertThat(csvHeaders.get("Email"), is(notNullValue()));
        assertThat(csvHeaders.get("Amount"), is(notNullValue()));
        assertThat(csvHeaders.get("Card Brand"), is(notNullValue()));
        assertThat(csvHeaders.get("Cardholder Name"), is(notNullValue()));
        assertThat(csvHeaders.get("Card Expiry Date"), is(notNullValue()));
        assertThat(csvHeaders.get("Card Number"), is(notNullValue()));
        assertThat(csvHeaders.get("State"), is(notNullValue()));
        assertThat(csvHeaders.get("Finished"), is(notNullValue()));
        assertThat(csvHeaders.get("Error Code"), is(notNullValue()));
        assertThat(csvHeaders.get("Error Message"), is(notNullValue()));
        assertThat(csvHeaders.get("Provider ID"), is(notNullValue()));
        assertThat(csvHeaders.get("GOV.UK Payment ID"), is(notNullValue()));
        assertThat(csvHeaders.get("Issued By"), is(notNullValue()));
        assertThat(csvHeaders.get("Date Created"), is(notNullValue()));
        assertThat(csvHeaders.get("Time Created"), is(notNullValue()));
        assertThat(csvHeaders.get("Corporate Card Surcharge"), is(notNullValue()));
        assertThat(csvHeaders.get("Wallet Type"), is(notNullValue()));
        assertThat(csvHeaders.get("Card Type"), is(notNullValue()));

        // Stripe specific should not available for other payment providers
        assertThat(csvHeaders.get("Net"), is(nullValue()));
        assertThat(csvHeaders.get("Fee"), is(nullValue()));
    }

    @Test
    public void getCsvHeadersShouldIncludeExternalMetadataFieldsInHeader() {
        TransactionEntity transactionEntity = transactionFixture.withTransactionDetails(
                new GsonBuilder().create().toJson(ImmutableMap.builder()
                        .put("external_metadata",
                                ImmutableMap.builder()
                                        .put("key-1", "value-1").put("key-2", "value-2").build())
                        .build()
                )).toEntity();

        TransactionEntity anotherTransactionEntity = transactionFixture.withTransactionDetails(
                new GsonBuilder().create().toJson(ImmutableMap.builder()
                        .put("external_metadata",
                                ImmutableMap.builder().put("key-0", "value-0").build())
                        .build()
                )).toEntity();

        List transactionEntities = List.of(transactionEntity, anotherTransactionEntity);

        Map<String, Object> csvHeaders = csvTransactionFactory.getCsvHeaders(transactionEntities);

        assertThat(csvHeaders.get("key-0 (metadata)"), is(notNullValue()));
        assertThat(csvHeaders.get("key-1 (metadata)"), is(notNullValue()));
        assertThat(csvHeaders.get("key-2 (metadata)"), is(notNullValue()));
    }

    @Test
    public void getCsvHeadersShouldIncludeFeeAndNetForStripePayments() {
        TransactionEntity transactionEntity = transactionFixture.withNetAmount(594)
                .withFee(6)
                .withTransactionDetails(
                        new GsonBuilder().create()
                                .toJson(ImmutableMap.builder()
                                        .put("payment_provider", "stripe")
                                        .build())
                )
                .toEntity();

        List transactionEntities = List.of(transactionEntity);

        Map<String, Object> csvHeaders = csvTransactionFactory.getCsvHeaders(transactionEntities);

        assertThat(csvHeaders.get("Net"), is(notNullValue()));
        assertThat(csvHeaders.get("Fee"), is(notNullValue()));
    }

    private void assertPaymentDetails(Map<String, Object> csvDataMap, TransactionEntity transactionEntity) {
        assertThat(csvDataMap.get("Reference"), is(transactionEntity.getReference()));
        assertThat(csvDataMap.get("Description"), is(transactionEntity.getDescription()));
        assertThat(csvDataMap.get("Email"), is(transactionEntity.getEmail()));
        assertThat(csvDataMap.get("Card Brand"), is("Visa"));
        assertThat(csvDataMap.get("Cardholder Name"), is(transactionEntity.getCardholderName()));
        assertThat(csvDataMap.get("Card Expiry Date"), is("10/24"));
        assertThat(csvDataMap.get("Card Number"), is(transactionEntity.getLastDigitsCardNumber()));
        assertThat(csvDataMap.get("Provider ID"), is(transactionEntity.getGatewayTransactionId()));
        assertThat(csvDataMap.get("GOV.UK Payment ID"), is(transactionEntity.getExternalId()));
        assertThat(csvDataMap.get("Card Type"), is("debit"));
        assertThat(csvDataMap.get("Wallet Type"), is("Apple Pay"));
    }
}