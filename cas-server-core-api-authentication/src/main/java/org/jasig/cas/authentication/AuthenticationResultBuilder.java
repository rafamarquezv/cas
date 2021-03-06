package org.jasig.cas.authentication;

import org.jasig.cas.authentication.principal.Service;

import java.io.Serializable;

/**
 * This is {@link AuthenticationResultBuilder}. It attempts to collect authentication objects
 * and will put the computed finalized primary {@link Authentication} into {@link AuthenticationResult}.
 *
 * <strong>Concurrency semantics: implementations MUST be thread-safe.</strong>
 * Instances of this class should never be declared as a field. Rather they should always be passedaround to methods that need them.
 *
 * @author Misagh Moayyed
 * @since 4.2.0
 */
public interface AuthenticationResultBuilder extends Serializable {

    /**
     * Collect authentication objects from any number of processed authentication transactions.
     *
     * @param authentication the authentication
     *
     * @return the authentication result builder
     */
    AuthenticationResultBuilder collect(Authentication authentication);

    /**
     * Build authentication result.
     *
     * @return the authentication result
     */
    AuthenticationResult build();

    /**
     * Build authentication result.
     *
     * @param service the service
     *
     * @return the authentication result
     */
    AuthenticationResult build(Service service);
}
