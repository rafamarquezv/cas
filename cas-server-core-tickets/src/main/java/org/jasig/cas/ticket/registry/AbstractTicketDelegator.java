package org.jasig.cas.ticket.registry;

import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.TicketGrantingTicket;

import java.time.ZonedDateTime;

import java.lang.reflect.Constructor;


/**
 * This is {@link AbstractTicketDelegator}.
 *
 * @author Misagh Moayyed
 * @since 4.2.0
 */
public abstract class AbstractTicketDelegator<T extends Ticket> implements Ticket {

    private static final long serialVersionUID = 1780193477774123440L;

    private final AbstractTicketRegistry ticketRegistry;

    private final T ticket;

    private final boolean callback;

    /**
     * Instantiates a new ticket delegator.
     *
     * @param ticketRegistry the ticket registry
     * @param ticket the ticket
     * @param callback the callback
     */
    protected AbstractTicketDelegator(final AbstractTicketRegistry ticketRegistry,
                                      final T ticket, final boolean callback) {
        this.ticketRegistry = ticketRegistry;
        this.ticket = ticket;
        this.callback = callback;
    }

    /**
     * Update ticket by the delegated registry.
     */
    protected void updateTicket() {
        this.ticketRegistry.updateTicket(this.ticket);
    }

    protected T getTicket() {
        return this.ticket;
    }

    @Override
    public final String getId() {
        return this.ticket.getId();
    }

    @Override
    public final boolean isExpired() {
        if (!callback) {
            return this.ticket.isExpired();
        }

        final TicketGrantingTicket t = getGrantingTicket();

        return this.ticket.isExpired() || (t != null && t.isExpired());
    }

    @Override
    public final TicketGrantingTicket getGrantingTicket() {
        final TicketGrantingTicket old = this.ticket.getGrantingTicket();

        if (old == null || !callback) {
            return old;
        }

        return this.ticketRegistry.getTicket(old.getId(), Ticket.class);
    }

    @Override
    public final ZonedDateTime getCreationTime() {
        return this.ticket.getCreationTime();
    }

    @Override
    public final int getCountOfUses() {
        return this.ticket.getCountOfUses();
    }

    @Override
    public int hashCode() {
        return this.ticket.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        return this.ticket.equals(o);
    }

    /**
     * Get the default constructor.
     *
     * @param clazz the ticket delegator class
     * @return the default constructor.
     */
    @SuppressWarnings("unchecked")
    public static Constructor<? extends AbstractTicketDelegator> getDefaultConstructor(final Class<? extends AbstractTicketDelegator>
                                                                                                   clazz) {
        try {
            return (Constructor<? extends AbstractTicketDelegator>) Class.forName(clazz.getName())
                    .getDeclaredConstructors()[0];
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
