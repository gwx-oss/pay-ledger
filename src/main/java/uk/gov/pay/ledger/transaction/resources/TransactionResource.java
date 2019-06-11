package uk.gov.pay.ledger.transaction.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.ledger.transaction.dao.TransactionDao;
import uk.gov.pay.ledger.transaction.model.Transaction;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/transaction")
@Produces(APPLICATION_JSON)
public class TransactionResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionResource.class);
    private final TransactionDao transactionDao;

    @Inject
    public TransactionResource(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    @Path("/{transactionExternalId}")
    @GET
    @Timed
    public Transaction getById(@PathParam("transactionExternalId") String transactionExternalId) {
        LOGGER.info("Get transaction request: {}", transactionExternalId);
        return transactionDao.findTransactionByExternalId(transactionExternalId)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }
}