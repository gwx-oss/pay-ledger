package uk.gov.pay.ledger.pact;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;
import org.junit.runner.RunWith;

@RunWith(PactRunner.class)
@Provider("ledger")
@PactBroker(scheme = "https", host = "${PACT_BROKER_HOST:pact-broker-test.cloudapps.digital}", tags = {"${PACT_CONSUMER_TAG}", "test"},
        authentication = @PactBrokerAuth(username = "${PACT_BROKER_USERNAME}", password = "${PACT_BROKER_PASSWORD}"),
        consumers = {"publicapi"})
public class PublicApiContractTest extends ContractTest {
}
