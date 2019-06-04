package uk.gov.pay.ledger.event;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.ledger.event.dao.EventDao;
import uk.gov.pay.ledger.event.model.Event;
import uk.gov.pay.ledger.event.resources.EventResource;

import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventResourceTest {
    private static final EventDao dao = mock(EventDao.class);
    private static final String eventId = "eventId";
    private static final String nonExistentId = "I'm not really here";
    private final Event event = new Event(eventId);

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new EventResource(dao))
            .build();

    @Before
    public void setup() {
        when(dao.getById(eq(eventId))).thenReturn(Optional.of(event));
    }

    @Test
    public void shouldReturnEventIfItExists() {
        Event returnedEvent = resources.target("/v1/event/" + eventId).request().get(Event.class);
        assertThat(returnedEvent.getId(), is(event.getId()));
    }

    @Test
    public void shouldReturn404IfEventDoesNotExist() {
        Response response = resources.target("/v1/event/" + nonExistentId).request().get();
        assertThat(response.getStatus(), is(404));
    }
}