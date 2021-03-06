package org.jasig.cas.logout;

import org.jasig.cas.services.LogoutType;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.util.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.net.URL;

/**
 * This is {@link DefaultSingleLogoutServiceMessageHandler} which handles the processing of logout messages
 * to logout endpoints processed by the logout manager.
 *
 * @author Misagh Moayyed
 * @since 4.3.0
 */
@Component("defaultSingleLogoutServiceMessageHandler")
public class DefaultSingleLogoutServiceMessageHandler implements SingleLogoutServiceMessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSingleLogoutServiceMessageHandler.class);

    /** The services manager. */
    @NotNull
    @Autowired
    @Qualifier("servicesManager")
    private ServicesManager servicesManager;

    /** An HTTP client. */
    @NotNull
    @Autowired
    @Qualifier("noRedirectHttpClient")
    private HttpClient httpClient;

    /**
     * Whether messages to endpoints would be sent in an asynchronous fashion.
     * True by default.
     **/
    @Value("${slo.callbacks.asynchronous:true}")
    private boolean asynchronous = true;

    @NotNull
    @Autowired
    @Qualifier("logoutBuilder")
    private LogoutMessageCreator logoutMessageBuilder;

    @Autowired
    @Qualifier("defaultSingleLogoutServiceLogoutUrlBuilder")
    private SingleLogoutServiceLogoutUrlBuilder singleLogoutServiceLogoutUrlBuilder;

    /**
     * Instantiates a new Single logout service message handler.
     */
    public DefaultSingleLogoutServiceMessageHandler() {}

    /**
     * Set if messages are sent in an asynchronous fashion.
     *
     * @param asyncCallbacks if message is synchronously sent
     * @since 4.1.0
     */
    public void setAsynchronous(final boolean asyncCallbacks) {
        this.asynchronous = asyncCallbacks;
    }

    /**
     * Handle logout for slo service.
     *
     * @param singleLogoutService the service
     * @param ticketId the ticket id
     * @return the logout request
     */
    @Override
    public LogoutRequest handle(final SingleLogoutService singleLogoutService, final String ticketId) {
        if (!singleLogoutService.isLoggedOutAlready()) {

            final RegisteredService registeredService = servicesManager.findServiceBy(singleLogoutService);
            if (serviceSupportsSingleLogout(registeredService)) {

                final URL logoutUrl = singleLogoutServiceLogoutUrlBuilder.determineLogoutUrl(registeredService, singleLogoutService);
                final DefaultLogoutRequest logoutRequest = new DefaultLogoutRequest(ticketId, singleLogoutService, logoutUrl);
                final LogoutType type = registeredService.getLogoutType() == null
                        ? LogoutType.BACK_CHANNEL : registeredService.getLogoutType();

                switch (type) {
                    case BACK_CHANNEL:
                        if (performBackChannelLogout(logoutRequest)) {
                            logoutRequest.setStatus(LogoutRequestStatus.SUCCESS);
                        } else {
                            logoutRequest.setStatus(LogoutRequestStatus.FAILURE);
                            LOGGER.warn("Logout message not sent to [{}]; Continuing processing...", singleLogoutService.getId());
                        }
                        break;
                    default:
                        logoutRequest.setStatus(LogoutRequestStatus.NOT_ATTEMPTED);
                        break;
                }
                return logoutRequest;
            }
        }
        return null;
    }

    /**
     * Log out of a service through back channel.
     *
     * @param request the logout request.
     * @return if the logout has been performed.
     */
    public boolean performBackChannelLogout(final LogoutRequest request) {
        try {
            final String logoutRequest = this.logoutMessageBuilder.create(request);
            final SingleLogoutService logoutService = request.getService();
            logoutService.setLoggedOutAlready(true);

            LOGGER.debug("Sending logout request for [{}] to [{}]", logoutService.getId(), request.getLogoutUrl());
            final LogoutHttpMessage msg = new LogoutHttpMessage(request.getLogoutUrl(), logoutRequest, this.asynchronous);
            LOGGER.debug("Prepared logout message to send is [{}]", msg);
            return this.httpClient.sendMessageToEndPoint(msg);
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * Service supports back channel single logout?
     * Service must be found in the registry. enabled and logout type must not be {@link LogoutType#NONE}.
     * @param registeredService the registered service
     * @return true, if support is available.
     */
    private boolean serviceSupportsSingleLogout(final RegisteredService registeredService) {
        return registeredService != null
                && registeredService.getAccessStrategy().isServiceAccessAllowed()
                && registeredService.getLogoutType() != LogoutType.NONE;
    }

    public void setServicesManager(final ServicesManager servicesManager) {
        this.servicesManager = servicesManager;
    }

    public void setHttpClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setLogoutMessageBuilder(final LogoutMessageCreator logoutMessageBuilder) {
        this.logoutMessageBuilder = logoutMessageBuilder;
    }

    public void setSingleLogoutServiceLogoutUrlBuilder(final SingleLogoutServiceLogoutUrlBuilder singleLogoutServiceLogoutUrlBuilder) {
        this.singleLogoutServiceLogoutUrlBuilder = singleLogoutServiceLogoutUrlBuilder;
    }

    public ServicesManager getServicesManager() {
        return servicesManager;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }

    public SingleLogoutServiceLogoutUrlBuilder getSingleLogoutServiceLogoutUrlBuilder() {
        return singleLogoutServiceLogoutUrlBuilder;
    }

    public LogoutMessageCreator getLogoutMessageBuilder() {
        return logoutMessageBuilder;
    }
}
