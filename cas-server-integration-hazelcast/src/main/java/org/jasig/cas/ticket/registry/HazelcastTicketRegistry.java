package org.jasig.cas.ticket.registry;

import org.jasig.cas.ticket.ServiceTicket;
import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.TicketGrantingTicket;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Hazelcast-based implementation of a {@link TicketRegistry}.
 * <p>This implementation just wraps the Hazelcast's {@link IMap}
 * which is an extension of the standard Java's {@code ConcurrentMap}.</p>
 * <p>The heavy lifting of distributed data partitioning, network cluster discovery and
 * join, data replication, etc. is done by Hazelcast's Map implementation.</p>
 *
 * @author Dmitriy Kopylenko
 * @author Jonathan Johnson
 * @since 4.1.0
 */
@Component("hazelcastTicketRegistry")
public class HazelcastTicketRegistry extends AbstractTicketRegistry implements DisposableBean {

    private final IMap<String, Ticket> registry;

    private final long serviceTicketTimeoutInSeconds;

    private final long ticketGrantingTicketTimeoutInSeconds;

    private final HazelcastInstance hz;


    /**
     * @param hz                                  An instance of {@code HazelcastInstance}
     * @param mapName                             Name of map to use
     * @param ticketGrantingTicketTimeoutInSeconds TTL for TGT entries
     * @param serviceTicketTimeoutInSeconds       TTL for ST entries
     */
    @Autowired
    public HazelcastTicketRegistry(
        @Qualifier("hazelcast")
        final HazelcastInstance hz,
        @Value("${hz.mapname:tickets}")
        final String mapName,
        @Value("${tgt.maxTimeToLiveInSeconds:28800}")
        final long ticketGrantingTicketTimeoutInSeconds,
        @Value("${st.timeToKillInSeconds:10}")
        final long serviceTicketTimeoutInSeconds) {

        this.registry = hz.getMap(mapName);
        this.ticketGrantingTicketTimeoutInSeconds = ticketGrantingTicketTimeoutInSeconds;
        this.serviceTicketTimeoutInSeconds = serviceTicketTimeoutInSeconds;
        this.hz = hz;
    }

    /**
     * Init.
     */
    @PostConstruct
    public void init() {
        logger.info("Setting up Hazelcast Ticket Registry...");
        logger.debug("Hazelcast instance: {} with name {}", this.hz, this.registry.getName());
        logger.debug("Ticket-granting ticket timeout: [{}s]", this.ticketGrantingTicketTimeoutInSeconds);
        logger.debug("Service ticket timeout: [{}s]", this.serviceTicketTimeoutInSeconds);
    }

    @Override
    protected void updateTicket(final Ticket ticket) {
        addTicket(ticket);
    }


    @Override
    protected boolean needsCallback() {
        return false;
    }



    @Override
    public void addTicket(final Ticket ticket) {
        addTicket(ticket, getTimeout(ticket));
    }

    /**
     * Adds the ticket to the hazelcast instance.
     *
     * @param ticket a ticket
     * @param ttl    time to live in seconds
     */
    private void addTicket(final Ticket ticket, final long ttl) {
        logger.debug("Adding ticket [{}] with ttl [{}s]", ticket.getId(), ttl);
        final Ticket encTicket = encodeTicket(ticket);
        this.registry.set(encTicket.getId(), encTicket, ttl, TimeUnit.SECONDS);
    }


    @Override
    public Ticket getTicket(final String ticketId) {
        final String encTicketId = encodeTicketId(ticketId);
        final Ticket ticket = decodeTicket(this.registry.get(encTicketId));
        return getProxiedTicketInstance(ticket);
    }


    @Override
    public boolean deleteSingleTicket(final String ticketId) {
        return this.registry.remove(ticketId) != null;
    }


    @Override
    public Collection<Ticket> getTickets() {
        return decodeTickets(this.registry.values());
    }

    /**
     * A method to get the starting TTL for a ticket based upon type.
     *
     * @param t Ticket to get starting TTL for
     * @return Initial TTL for ticket
     */
    private long getTimeout(final Ticket t) {
        if (t instanceof TicketGrantingTicket) {
            return this.ticketGrantingTicketTimeoutInSeconds;
        }
        if (t instanceof ServiceTicket) {
            return this.serviceTicketTimeoutInSeconds;
        }

        throw new IllegalArgumentException(
            String.format("Invalid ticket type [%s]. Expecting either [TicketGrantingTicket] or [ServiceTicket]",
                t.getClass().getName()));
    }

    /**
     * Make sure we shutdown HazelCast when the context is destroyed.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Hazelcast instance {}", hz.getConfig().getInstanceName());
        this.hz.shutdown();
    }

    @Override
    public void destroy() throws Exception {
        shutdown();
    }
}
