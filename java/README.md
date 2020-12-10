Mobile ID Java client
=====================

Swisscom Mobile ID is cost-efficient, managed authentication service from Swisscom. The customer-facing API is
based on open standard ETSI 102 2041. The library from this repository is a reference implementation for 
a Java client that provides access to the main services provided by Mobile ID:

* Mobile digital signature (sync and async)
* Signature receipt
* Mobile user profile query

The repository provides implementation for the REST and SOAP protocols. The user is free to choose one (or both) of them.

## General info
The following modules are built in:

* _mobileid-client-core_: the main classes that the user works with. Provides a simplified model to work with
* _mobileid-client-rest_: the REST implementation of the client. Uses the REST API of Mobile ID.
* _mobileid-client-soap_: the SOAP implementation of the client. Uses the SOAP API of Mobile ID.
* _mobileid-client-samples_: samples of configuring and using the client library

The client library is production ready and implementation with thread safety and resource pooling in mind.
The REST implementation provides good HTTP connection pooling while the SOAP implementation reuses 
the SOAP WS ports in a careful way. As general recommended practice, you should create and configure one 
single instance of the Mobile ID client and reuse it anywhere in your project. 
In general, the client is Spring-friendly.

## Configuration sample
To get access to the Mobile ID client classes, add it as dependency to your project or module:
```xml
<dependencies>
    <dependency>
        <groupId>ch.swisscom.mid.client</groupId>
        <artifactId>mid-client-rest</artifactId>
        <version>1.0</version>
    </dependency>
    <!-- or if you want the SOAP version :
    <dependency>
        <groupId>ch.swisscom.mid.client</groupId>
        <artifactId>mid-client-soap</artifactId>
        <version>1.0</version>
    </dependency>
    -->
</dependencies>
```

Next, you should configure the client:
```java
ClientConfiguration config = new ClientConfiguration();
config.setProtocolToRest();
config.setApId("mid://id-received-from-swisscom");
config.setApPassword("pass-received-from-swisscom");

UrlsConfiguration urls = config.getUrls();
urls.setAllServiceUrlsTo(DefaultConfiguration.DEFAULT_INTERNET_BASE_URL + DefaultConfiguration.REST_ENDPOINT_SUB_URL);

TlsConfiguration tls = config.getTls();
tls.setKeyStoreFile("/home/localuser/mid_keystore.jks");
tls.setKeyStorePassword("secret");
tls.setKeyStoreKeyPassword("secret");
tls.setKeyStoreCertificateAlias("mid-cert");
tls.setTrustStoreFile("/home/localuser/truststore.jks");
tls.setTrustStorePassword("secret");

HttpConfiguration http = config.getHttp();
http.setConnectionTimeoutInMs(20 * 1000);
http.setResponseTimeoutInMs(100 * 1000);

MIDClient client = new MIDClientImpl(clientConfig);
```

Finally, test the client:
```java
SignatureRequest request = new SignatureRequest();
request.setUserLanguage(UserLanguage.ENGLISH);
request.getDataToBeSigned().setData("Testing: Please sign this document");
request.getMobileUser().setMsisdn("41**********");
request.setSignatureProfile(SignatureProfiles.DEFAULT_PROFILE);

SignatureResponse response = client.requestSyncSignature(request);
System.out.println(response.toString());
```

## Points of interest
The user API of the Mobile ID client is generally very simple. The model provided is a 
simplified model of the normal ETSI 204 model, while still allowing for full flexibility and
access to most functions. 

To help with using the service, the following classes might be of interest to you:

* ch.swisscom.mid.client.config.DefaultConfiguration: provides sensible defaults for most constants used by the library.
* ch.swisscom.mid.client.config.TrafficObserver: gives you access to the request and response JSON or XML messages exchanged with the server. You might need this if you want to log/post-process/analyze the exchanged messages.
* ch.swisscom.mid.client.model.SignatureProfiles: provides a list of the common signature profiles that Mobile ID is using.
* ch.swisscom.mid.client.model.TrialNumbers: provides a list of phone numbers (MSISDNs) that can be used for various testing purposes.
