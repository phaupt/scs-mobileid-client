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
package ch.swisscom.mid.client.model;

public class ProfileMobileUserCertificate {

    private ProfileMobileUserCertificateState state;

    private String algorithm;

    private String certificateAsBase64;

    private String subjectNameAsBase64;

    private String caCertificateAsBase64;

    private String caSubjectNameAsBase64;

    public ProfileMobileUserCertificateState getState() {
        return state;
    }

    public void setState(ProfileMobileUserCertificateState state) {
        this.state = state;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getCertificateAsBase64() {
        return certificateAsBase64;
    }

    public void setCertificateAsBase64(String certificateAsBase64) {
        this.certificateAsBase64 = certificateAsBase64;
    }

    public String getSubjectNameAsBase64() {
        return subjectNameAsBase64;
    }

    public void setSubjectNameAsBase64(String subjectNameAsBase64) {
        this.subjectNameAsBase64 = subjectNameAsBase64;
    }

    public String getCaCertificateAsBase64() {
        return caCertificateAsBase64;
    }

    public void setCaCertificateAsBase64(String caCertificateAsBase64) {
        this.caCertificateAsBase64 = caCertificateAsBase64;
    }

    public String getCaSubjectNameAsBase64() {
        return caSubjectNameAsBase64;
    }

    public void setCaSubjectNameAsBase64(String caSubjectNameAsBase64) {
        this.caSubjectNameAsBase64 = caSubjectNameAsBase64;
    }

    @Override
    public String toString() {
        return "ProfileMobileUserCertificate{" +
               "state=" + state +
               ", algorithm='" + algorithm + '\'' +
               ", certificateAsBase64='" + (certificateAsBase64 == null ? "null" : "(not-null)") + '\'' +
               ", subjectNameAsBase64='" + subjectNameAsBase64 + '\'' +
               ", caCertificateAsBase64='" + (caCertificateAsBase64 == null ? "null" : "(not-null)") + '\'' +
               ", caSubjectNameAsBase64='" + caSubjectNameAsBase64 + '\'' +
               '}';
    }
}
