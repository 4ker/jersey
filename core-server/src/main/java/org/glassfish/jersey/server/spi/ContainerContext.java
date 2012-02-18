/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.server.spi;

import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ContainerException;

/**
 * A suspendable, request-scoped container context.
 *
 * Container sends a new instance of the context with every request as part of the
 * call to the Jersey application {@link Application#apply(javax.ws.rs.core.Request,
 * ContainerContext) apply(...)} method.
 *
 * @author Marek Potociar
 */
public interface ContainerContext {

    /**
     * Time-out handler can be registered when the container context gets suspended.
     *
     * Should the suspend operation time out, the container is responsible for
     * invoking the {@link TimeoutHandler#onTimeout(org.glassfish.jersey.server.ContainerContext)}
     * callback method to get the response that should be returned to the client.
     */
    public interface TimeoutHandler {

        /**
         * Method is called, when {@link ContainerContext#suspend(long, TimeUnit,
         * ContainerContext.TimeoutHandler) ContainerContext.suspend(...)} operation
         * times out.
         *
         * The custom time-out handler implementation is responsible for making
         * sure a (time-out) response is written to the context and that the
         * container context is properly closed.
         * <p />
         * The result of the context {@link ContainerContext#resume()} method
         * may be used to resolve potential response writing race condition
         * between an application layer resume event and the processed time-out
         * event.
         *
         * @param context suspended container context that timed out
         */
        public void onTimeout(ContainerContext context);
    }

    /**
     * Write the status and headers of the response and return an output stream
     * for the web application to write the entity of the response.
     *
     * @param contentLength >=0 if the content length in bytes of the
     *        entity to be written is known, otherwise -1. Containers
     *        may use this value to determine whether the "Content-Length"
     *        header can be set or utilize chunked transfer encoding.
     * @param response the container response. The status and headers are
     *        obtained from the response.
     * @return the output stream to write the entity (if any).
     * @throws ContainerException if an error occurred when writing out the
     *         status and headers or obtaining the output stream.
     */
    public OutputStream writeResponseStatusAndHeaders(long contentLength, Response response) throws ContainerException;

    /**
     * Suspend the request/response processing.
     *
     * Container should close the associated thread(s) but keep the network
     * connection open so that the response can be sent later. This method must
     * not be invoked more than once, otherwise an exception is thrown.
     *
     * @param timeOut time-out value.
     * @param timeUnit time-out time unit.
     * @param timeoutHandler time-out handler to process a time-out event if it
     *     occurs.
     * @throws IllegalStateException in case the container has already been suspended.
     */
    public void suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler) throws IllegalStateException;

    /**
     * Resume the container context.
     *
     * This indicates to the container that the
     * {@link org.glassfish.jersey.server.Application Jersey application}
     * is ready to send a response back to the client. The method must be
     * implemented as thread-safe.
     * <p />
     * The context will return {@code true} if it has been suspended previously
     * and has not been resumed yet. If the context was resumed already, it will
     * return {@link false}.
     * <p />
     * The result returned by this method allows to synchronize the the main
     * request-response processing flow in the application with the code that
     * implements {@link TimeoutHandler#onTimeout(ContainerContext) context time-out event}
     * reconciliation processing. E.g. it is possible that the time out event occurs
     * together with the application-layer resume event. In such case, the boolean
     * value returned from the {@code resume()} method will help to decide which
     * code should proceed with writing the response and which code should back
     * out.
     *
     * @return {@code true} if the suspended context was successfully resumed and
     *     the response can be written, otherwise returns {@code false}.
     */
    public boolean resume();

    /**
     * Close the context.
     *
     * Indicates to the container that request has been fully processed and response
     * has been fully written. This enables the container context to finish processing,
     * clean up any state, flush any streams, etc.
     */
    void close();
}