/**
 * Copyright (c) 2016 OpenCloud
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   1. Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *   3. The name of the author may not be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 *   4. The source code may not be used to create, develop, use or distribute
 *      software for use on any platform other than the OpenCloud Rhino
 *      platform or any successor products.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Visit OpenCloud Developer's Portal for how-to guides, examples,
 * documentation, forums and more: http://developer.opencloud.com
 */
package com.opencloud.slee.services.easysip.registrar;

import org.jainslee.resources.sip.*;

import javax.slee.ActivityContextInterface;
import javax.slee.InitialEventSelector;
import javax.slee.SbbContext;
import javax.slee.facilities.TraceLevel;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.opencloud.slee.services.easysip.common.BaseSbb;
import com.opencloud.slee.services.easysip.common.EasySipUtils;

/**
 * This SBB handles a SIP Register message and replies with
 * a successful message.
 *
 * @slee.service
 *     description="Registrar Service"
 *     name="@service.name@"
 *     vendor="@service.vendor@"
 *     version="@service.version@"
 *     default-priority="0"
 *
 * @slee.sbb
 *     id="rsbbid"
 *     name="@sbb.name@"
 *     vendor="@sbb.vendor@"
 *     version="@sbb.version@"
 *
 * @slee.ra-type-binding
 *      resource-adaptor-type-name="@easysip.ratype.name@"
 *      resource-adaptor-type-vendor="@easysip.ratype.vendor@"
 *      resource-adaptor-type-version="@easysip.ratype.version@"
 *      activity-context-interface-factory-name="@easysip.sis.acifactory.name@"
 *      resource-adaptor-object-name="@easysip.sis.provider.name@"
 *      resource-adaptor-entity-link="@easysip.sis.linkname@"
 */
public abstract class RegistrarSbb extends BaseSbb {
    public void setSbbContext(SbbContext context) {
        super.setSbbContext(context);
        try {
            sipFactory = (SipFactory) new InitialContext().lookup("java:comp/env/slee/resources/easysip/sipfactory");
        } catch (NamingException e) {
            severe("failed to set SBB context", e);
        }
    }

    /**
     * The Initial Event Selector method.
     * @param ies a InitialEventSelector
     * @return a InitialEventSelector
     */
    public InitialEventSelector initialEventSelect(InitialEventSelector ies) {
        finest("initialEventSelect");
        
        if (ies.getEvent() instanceof IncomingSipRequest) {
            IncomingSipRequest request = ((IncomingSipRequest) ies.getEvent());
            if (request.getMethod().equals(IncomingSipRequest.REGISTER)) {
                // the only request handled by RegistrarSbb
                Address address = null;
                try {
                    address = request.getAddressHeader("To");
                } catch (SipParseException e) {
                    ies.setInitialEvent(false);
                }
                if (address != null) {
                    if (isFineEnabled()) {
                        fine("IES using custom convergence name: '"
                                + address.getURI() + "'");
                    }
                    ies.setCustomName(address.getURI().toString());
                }
            } else {
                // others sbbs would handle this
                ies.setInitialEvent(false);
            }
        }
        return ies;
    }
    
    /**
     * Event Handler method for Register.
     * @slee.event-method
     *     event-type-name="org.jainslee.resources.sip.IncomingSipRequest.REGISTER"
     *     event-type-vendor="@easysip.ratype.vendor@"
     *     event-type-version="@easysip.ratype.version@"
     *     initial-event="True"
     *     initial-event-selector-method-name="initialEventSelect"
     *
     * @param event a IncomingSipRequest
     * @param aci a ActivityContextInterface
     */
    public void onRegister(IncomingSipRequest request,
            ActivityContextInterface aci) {
        if (isTraceable(TraceLevel.FINEST))
            finest("onRegisterEvent: received request:\n" + request);
        
        aci.detach(getSbbLocalObject());
        try {
            // RFC3261 10.3
            
            // Is this request for this domain?
            // TODO not for this domain - proxy it
            
            // Process require header
            // Authenticate
            // Authorize
            // OK we're authorized now ;-)
            
            URI uri = request.getAddressHeader("To").getURI();
            String sipAddressOfRecord = EasySipUtils.getCanonicalAddress(uri);
            
            if (request.getAddressHeader("Contact") == null) {
                // This was only a query
                if (isTraceable(TraceLevel.FINE))
                    fine("received registration query for: "
                            + sipAddressOfRecord);
                sendFinalResponse(request, SipResponse.SC_OK, -1, null);
                return;
            }
            
            URI contactURI = request.getAddressHeader("Contact").getURI();
            int expires = request.getExpires();
            
            if (expires > 0)
            {
                if (isTraceable(TraceLevel.FINE))
                    fine("register received");
            }
            else
            {
                if (isTraceable(TraceLevel.FINE))
                    fine("unregister received");
            }
            sendFinalResponse(request, SipResponse.SC_OK, expires,
                    contactURI.toString());
            
        } catch (SipParseException e) {
            warning("Unable to process registration", e);
            sendFinalResponse(request, SipResponse.SC_SERVER_INTERNAL_ERROR, -1, null);
        }
    }

    /**
     * Sends a response to Register request.
     * 
     * @param request received request
     * @param statusCode the response code
     * @param expires expiration time
     * @param locationAddress the location address
     */
    private void sendFinalResponse(IncomingSipRequest request, int statusCode,
            int expires, String locationAddress) {
        fine("sendFinalResponse");
        try {
            OutgoingSipResponse response = request.createResponse(statusCode);
            
            if (locationAddress != null) {
                Address contactAddr = sipFactory.createAddress(locationAddress);
                if (expires != -1) {
                    contactAddr.setExpires(expires);
                    response.addAddressHeader("Contact", contactAddr, false);
                }
            }
            response.setHeader("Date",
                    EasySipUtils.toSipDate(System.currentTimeMillis()));
            
            if (isTraceable(TraceLevel.FINEST))
                finest("sending response:\n" + response);
            
            response.send();
        } catch (Exception e) {
            severe("Unable to send " + statusCode + " response", e);
        }
    }

    private SipFactory sipFactory;
}
