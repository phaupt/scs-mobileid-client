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

import org.etsi.uri.ts102204.v1_1.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import ch.swisscom.mid.client.config.DefaultConfiguration;
import ch.swisscom.mid.client.model.*;
import ch.swisscom.mid.ts102204.as.v1.SubscriberInfoDetail;
import fi.ficom.mss.ts102204.v1_0.ServiceResponses;
import fi.methics.ts102204.ext.v1_0.CertificateType;
import fi.methics.ts102204.ext.v1_0.MobileUserType;
import fi.methics.ts102204.ext.v1_0.PinStatusType;
import fi.methics.ts102204.ext.v1_0.ProfileQueryExtension;
import fi.methics.ts102204.ext.v1_0.SscdListType;
import fi.methics.ts102204.ext.v1_0.SscdType;

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

    public static ProfileResponse processMssProfileQueryResponse(MSSProfileRespType mssResponse) {
        ProfileResponse response = new ProfileResponse();
        if (mssResponse.getSignatureProfile() != null) {
            response.setSignatureProfiles(
                mssResponse.getSignatureProfile().stream().map(MssURIType::getMssURI).collect(Collectors.toList()));
        }
        if (mssResponse.getStatus() != null &&
            mssResponse.getStatus().getStatusDetail() != null &&
            mssResponse.getStatus().getStatusDetail().getRegistrationOutputOrEncryptedRegistrationOutputOrEncryptionCertificates() != null) {
            List<Object> profileQueryExtensions = mssResponse
                .getStatus()
                .getStatusDetail()
                .getRegistrationOutputOrEncryptedRegistrationOutputOrEncryptionCertificates();
            if (profileQueryExtensions.size() == 1) {
                ProfileQueryExtension mssPQExt = (ProfileQueryExtension) profileQueryExtensions.get(0);
                if (mssPQExt.getMobileUser() != null) {
                    ProfileMobileUserInfo mobileUserInfo = new ProfileMobileUserInfo();
                    MobileUserType mssPQMobileUser = mssPQExt.getMobileUser();
                    if (mssPQMobileUser != null) {
                        mobileUserInfo.setRecoveryCodeCreated(mssPQMobileUser.isRecoveryCodeCreated());
                        mobileUserInfo.setAutoActivation(Boolean.parseBoolean(
                            mssPQMobileUser.getOtherAttributes().get(new QName("AutoActivation"))));
                    }
                    response.setMobileUser(mobileUserInfo);
                }
                if (mssPQExt.getSscds() != null) {
                    response.setSimDevices(new ArrayList<>());
                    response.setAppDevices(new ArrayList<>());
                    SscdListType mssSscds = mssPQExt.getSscds();
                    if (mssSscds.getSim() != null) {
                        SscdType mssSim = mssSscds.getSim();
                        response.getSimDevices().add(processDeviceInfo(mssSim::getState,
                                                                       mssSim::getPinStatus,
                                                                       mssSim::getMobileUserCertificate));
                    }
                    if (mssSscds.getApp() != null && mssSscds.getApp().size() > 0) {
                        List<SscdType> mssAppList = mssSscds.getApp();
                        for (SscdType mssApp : mssAppList) {
                            response.getAppDevices().add(processDeviceInfo(mssApp::getState,
                                                                           mssApp::getPinStatus,
                                                                           mssApp::getMobileUserCertificate));
                        }
                    }
                }
            }
        }
        return response;
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
        List<AdditionalServiceResponse> resultList = new ArrayList<>();
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

    @SuppressWarnings("unchecked")
    private static ProfileDeviceInfo processDeviceInfo(Supplier<String> stateSupplier,
                                                       Supplier<PinStatusType> pinStatusSupplier,
                                                       Supplier<List<CertificateType>> certificateListSupplier) {
        ProfileDeviceInfo deviceInfo = new ProfileDeviceInfo();
        deviceInfo.setState(ProfileDeviceState.getByStateString(stateSupplier.get()));
        if (pinStatusSupplier.get() != null) {
            deviceInfo.setPinState(ProfileDevicePinState.getByPinBlockedBooleanValue(pinStatusSupplier.get().isBlocked()));
        }
        if (certificateListSupplier.get() != null && certificateListSupplier.get().size() > 0) {
            deviceInfo.setCertificates(new ArrayList<>());
            for (CertificateType mssCert : certificateListSupplier.get()) {
                ProfileMobileUserCertificate cert = new ProfileMobileUserCertificate();
                cert.setAlgorithm(mssCert.getAlgorithm());
                cert.setState(ProfileMobileUserCertificateState.getByStateString(mssCert.getState()));

                List<Object> mssCertElementList = mssCert.getX509IssuerSerialOrX509SKIOrX509SubjectName();
                if (mssCertElementList != null && mssCertElementList.size() > 0) {
                    CertificateData certificateData = new CertificateData();
                    certificateData.setCertificateAsBase64(new String(((JAXBElement<byte[]>) mssCertElementList.get(0)).getValue(),
                                                                      StandardCharsets.UTF_8));
                    if (mssCertElementList.size() > 1) {
                        certificateData.setSubjectName(((JAXBElement<String>) mssCertElementList.get(1)).getValue());
                    }
                    cert.setUserCertificate(certificateData);
                    List<CertificateData> caCertificates = new ArrayList<>();
                    if (mssCertElementList.size() > 2) {
                        for (int index = 2; index < mssCertElementList.size(); index += 2) {
                            certificateData = new CertificateData();
                            certificateData.setCertificateAsBase64(new String(((JAXBElement<byte[]>) mssCertElementList.get(index)).getValue(),
                                                                              StandardCharsets.UTF_8));
                            if (index + 1 < mssCertElementList.size()) {
                                certificateData.setSubjectName(((JAXBElement<String>) mssCertElementList.get(index + 1)).getValue());
                            }
                            caCertificates.add(certificateData);
                        }
                    }
                    cert.setCaCertificates(caCertificates);
                }
                deviceInfo.getCertificates().add(cert);
            }
        }
        return deviceInfo;
    }

}
