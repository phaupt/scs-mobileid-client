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
package ch.swisscom.mid.client.soap.adapter;

import org.etsi.uri.ts102204.v1_1.MSSSignatureRespType;
import org.etsi.uri.ts102204.v1_1.MssURIType;
import org.etsi.uri.ts102204.v1_1.SignatureType;
import org.etsi.uri.ts102204.v1_1.StatusCodeType;
import org.etsi.uri.ts102204.v1_1.StatusDetailType;
import org.etsi.uri.ts102204.v1_1.StatusType;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import ch.swisscom.mid.client.config.DefaultConfiguration;
import ch.swisscom.mid.client.model.AdditionalServiceResponse;
import ch.swisscom.mid.client.model.SignatureResponse;
import ch.swisscom.mid.client.model.Status;
import ch.swisscom.mid.client.model.StatusCode;
import ch.swisscom.mid.client.model.SubscriberInfoAdditionalServiceResponse;
import ch.swisscom.mid.ts102204.as.v1.SubscriberInfoDetail;
import fi.ficom.mss.ts102204.v1_0.ServiceResponses;

public class MssResponseProcessor {

    public static SignatureResponse processMssSignatureResponse(MSSSignatureRespType mssResponse) {
        SignatureResponse signatureResponse = new SignatureResponse();
        if (mssResponse != null) {
            MssURIType mssSigProfileUri = mssResponse.getSignatureProfile();
            if (mssSigProfileUri != null) {
                signatureResponse.setSignatureProfile(mssSigProfileUri.getMssURI());
            }
            SignatureType mssSignature = mssResponse.getMSSSignature();
            if (mssSignature != null) {
                signatureResponse.setBase64Signature(new String(mssSignature.getBase64Signature(), StandardCharsets.UTF_8));
            }
            StatusType mssResponseStatus = mssResponse.getStatus();
            signatureResponse.setStatus(processStatus(mssResponseStatus));
            signatureResponse.setAdditionalServiceResponses(processAdditionalServiceResponses(mssResponseStatus));
        } else {
            signatureResponse.setStatus(getGenericErrorStatus("Invalid MSS Signature response (it was NULL)"));
        }
        return signatureResponse;
    }

    // ----------------------------------------------------------------------------------------------------

    private static Status processStatus(StatusType mssStatus) {
        if (mssStatus != null) {
            Status result = new Status();
            result.setStatusMessage(mssStatus.getStatusMessage());
            StatusCodeType mssStatusCode = mssStatus.getStatusCode();
            if (mssStatusCode != null && mssStatusCode.getValue() != null) {
                int statusCodeValue = mssStatusCode.getValue().intValue();
                StatusCode statusCode = StatusCode.getByStatusCodeValue(statusCodeValue);
                if (statusCode != null) {
                    result.setStatusCode(statusCode);
                    result.setStatusCodeString(statusCode.name());
                }
            }
            return result;
        } else {
            return getGenericErrorStatus("Invalid MSS Signature response status");
        }
    }

    private static List<AdditionalServiceResponse> processAdditionalServiceResponses(StatusType mssStatus) {
        List<AdditionalServiceResponse> resultList = new LinkedList<>();

        if (mssStatus != null) {
            StatusDetailType mssStatusDetail = mssStatus.getStatusDetail();
            if (mssStatusDetail != null) {
                List<Object> mssResponseList = mssStatusDetail.getRegistrationOutputOrEncryptedRegistrationOutputOrEncryptionCertificates();
                for (Object mssResponse : mssResponseList) {
                    if (mssResponse instanceof ServiceResponses) {
                        ServiceResponses mssServiceResponses = (ServiceResponses) mssResponse;
                        if (mssServiceResponses.getServiceResponse() != null &&
                            mssServiceResponses.getServiceResponse().size() > 0) {
                            ServiceResponses.ServiceResponse mssServiceResponse = mssServiceResponses.getServiceResponse().get(0);
                            if (mssServiceResponse.getDescription() != null &&
                                mssServiceResponse.getDescription().getMssURI() != null) {
                                String mssServiceUri = mssServiceResponse.getDescription().getMssURI();
                                if (DefaultConfiguration.ADDITIONAL_SERVICE_SUBSCRIBER_INFO_URI.equals(mssServiceUri)
                                    && mssServiceResponse.getSubscriberInfo() != null
                                    && mssServiceResponse.getSubscriberInfo().getDetail() != null
                                    && mssServiceResponse.getSubscriberInfo().getDetail().size() > 0) {
                                    SubscriberInfoDetail mssSubInfoDetail = mssServiceResponse.getSubscriberInfo().getDetail().get(0);
                                    SubscriberInfoAdditionalServiceResponse asResponse = new SubscriberInfoAdditionalServiceResponse();
                                    asResponse.setResponseId(mssSubInfoDetail.getId());
                                    asResponse.setResponseValue(mssSubInfoDetail.getValue());
                                    resultList.add(asResponse);
                                }
                            }
                        }
                    }
                }
            }
        }
        return resultList;
    }

    private static Status getGenericErrorStatus(String statusMessage) {
        Status status = new Status();
        status.setStatusCode(StatusCode.INTERNAL_ERROR);
        status.setStatusMessage(statusMessage);
        return status;
    }

}
