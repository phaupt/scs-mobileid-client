/*
 * Copyright 2021 Swisscom (Schweiz) AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.swisscom.mid.client.soap;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.etsi.uri.ts102204.etsi204_kiuru.MSSSignaturePortType;
import org.etsi.uri.ts102204.v1_1.MSSSignatureReqType;
import org.etsi.uri.ts102204.v1_1.MSSSignatureRespType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.soap.SOAPFaultException;

import ch.swisscom.mid.client.MIDFlowException;
import ch.swisscom.mid.client.config.ClientConfiguration;
import ch.swisscom.mid.client.config.ComProtocol;
import ch.swisscom.mid.client.config.TrafficObserver;
import ch.swisscom.mid.client.impl.ComProtocolHandler;
import ch.swisscom.mid.client.impl.Loggers;
import ch.swisscom.mid.client.model.*;
import ch.swisscom.mid.client.soap.adapter.MssFaultProcessor;
import ch.swisscom.mid.client.soap.adapter.MssModelBuilder;
import ch.swisscom.mid.client.soap.adapter.MssResponseProcessor;

public class ComProtocolHandlerSoapImpl implements ComProtocolHandler {

    private static final Logger logClient = LoggerFactory.getLogger(Loggers.LOGGER_CLIENT);
    private static final Logger logConfig = LoggerFactory.getLogger(Loggers.LOGGER_CONFIG);
    private static final Logger logProtocol = LoggerFactory.getLogger(Loggers.LOGGER_CLIENT_PROTOCOL);

    private ClientConfiguration config;
    private ObjectPool<MssService<MSSSignaturePortType>> mssSignatureServicePool;

    @Override
    public ComProtocol getImplementedComProtocol() {
        return ComProtocol.SOAP;
    }

    @Override
    public void initialize(ClientConfiguration config) {
        this.config = config;
        mssSignatureServicePool = new GenericObjectPool<>(new MssServiceFactory<>(config,
                                                                                  MSSSignaturePortType.class,
                                                                                  config.getUrls()::getSignatureServiceUrl));
        logConfig.info("Initializing MID SOAP client with config: [{}]", config);
    }

    @Override
    public void close() {
        // no code here
    }

    // ----------------------------------------------------------------------------------------------------

    @Override
    public SignatureResponse requestSyncSignature(SignatureRequest request) {
        logProtocol.info("MSS Signature: Sending request: [{}]", request);
        MSSSignatureReqType mssSignatureReq = MssModelBuilder.createSignatureReqType(config, request, true);
        notifyTrafficObserverForApTransId(request.getTrafficObserver(), mssSignatureReq.getAPInfo().getAPTransID());
        MSSSignatureRespType mssSignatureResp;
        MssService<MSSSignaturePortType> mssSignatureService = null;
        try {
            mssSignatureService = mssSignatureServicePool.borrowObject();
            mssSignatureService.registerTrafficObserverForThisRequest(request.getTrafficObserver());
            mssSignatureResp = mssSignatureService.getPort().mssSignature(mssSignatureReq);
            logClient.info("Received MSS signature response: [{}]", mssSignatureResp == null ? "null" : "not-null, looks OK");
        } catch (SOAPFaultException e) {
            throw new MIDFlowException("SOAP Fault received", e, MssFaultProcessor.processSoapFaultException(e));
        } catch (Exception e) {
            throw new MIDFlowException("Error in Signature operation.", e, MssFaultProcessor.processException(e, FailureReason.MID_SERVICE_FAILURE));
        } finally {
            if (mssSignatureService != null) {
                try {
                    mssSignatureService.clearTrafficObserver();
                    mssSignatureServicePool.returnObject(mssSignatureService);
                } catch (Exception e) {
                    logClient.error("Failed to return MSS Signature Port object back to the pool", e);
                }
            }
        }
        return MssResponseProcessor.processMssSignatureResponse(mssSignatureResp);
    }

    @Override
    public SignatureResponse requestAsyncSignature(SignatureRequest request) {
        throw new UnsupportedOperationException("Async signature is not yet supported");
    }

    @Override
    public SignatureResponse pollForSignatureStatus(SignatureTracking signatureTracking) {
        throw new UnsupportedOperationException("Signature status poll is not yet supported");
    }

    @Override
    public ReceiptResponse requestSyncReceipt(SignatureTracking signatureTracking, ReceiptRequest request) {
        throw new UnsupportedOperationException("Receipt signature is not yet supported");
    }

    @Override
    public ProfileResponse requestProfile(ProfileRequest request) {
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    private void notifyTrafficObserverForApTransId(TrafficObserver trafficObserver, String apTransId) {
        if (trafficObserver == null) {
            return;
        }
        trafficObserver.notifyOfGeneratedApTransId(apTransId, ComProtocol.REST);
    }

}
