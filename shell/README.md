mobileid: shell
============

Contains shell scripts to invoke a:

* Signature Request
* Receipt Request
* Profile Query Request

## Configuration
The file `mobileid.properties` contains the most relevant configuration properties that need to be adjusted before the scripts are used.
Note that you will require the valid SSL certificate files `mycert.crt` and `mycert.key` in order to connect to the Mobile ID service.

## Usage 
### Signature Request
```
Usage: ./mobileid-sign.sh <args> mobile 'message' userlang <receipt>
  -t value   - message type (SOAP, JSON); default SOAP
  -s value   - signature profile to select the authentication method; default http://mid.swisscom.ch/MID/v1/AuthProfile1
               possible values:
               http://mid.swisscom.ch/MID/v1/AuthProfile1 = alias of http://mid.swisscom.ch/STK-LoA4
               http://mid.swisscom.ch/Any-LoA4            = sim authentication preferred. fallback to app authentication method
               http://mid.swisscom.ch/STK-LoA4            = force sim authentication
               http://mid.swisscom.ch/Device-LoA4         = force app authentication
  -v         - verbose output
  -d         - debug mode
  mobile     - mobile number
  message    - message to be signed (and displayed)
               A placeholder #TRANSID# may be used anywhere in the message to include a unique transaction id
  userlang   - user language (one of en, de, fr, it)
  receipt    - optional success receipt message

  Example ./mobileid-sign.sh -v +41792080350 'test.com: Do you want to login to corporate VPN? (#TRANSID#)' en
          ./mobileid-sign.sh -t JSON -v +41792080350 'test.com: Do you want to login to corporate VPN? (#TRANSID#)' en
          ./mobileid-sign.sh -s 'http://mid.swisscom.ch/Device-LoA4' -v +41792080350 'test.com: Do you want to login to corporate VPN? (#TRANSID#)' en
          ./mobileid-sign.sh -v +41792080350 'test.com: Do you want to login to corporate VPN? (#TRANSID#)' en 'test.com: Successful login into VPN'
```

### Receipt Request
```
Usage: ./mobileid-receipt.sh <args> mobile transID 'message' userlang
  -t value   - message type (SOAP, JSON); default SOAP
  -v         - verbose output
  -d         - debug mode
  mobile     - mobile number
  transID    - transaction id of the related signature request
  message    - message to be displayed
  userlang   - user language (one of en, de, fr, it)

  Example ./mobileid-receipt.sh -v +41792080350 h29ah1 'Successful login into VPN' en
          ./mobileid-receipt.sh -t JSON -v +41792080350 h29ah1 'Successful login into VPN' en
```

### Profile Query Request
```
Usage: ./mobileid-query.sh <args> mobile
  -t value   - message type (SOAP, JSON); default SOAP
  -v         - verbose output
  -d         - debug mode
  mobile     - mobile number

  Example ./mobileid-query.sh -v +41792080350
          ./mobileid-query.sh -t JSON -v +41792080350
````

## Example Outputs (verbose mode)
### Successful Signature

```
phaupt@AWS:~/NEW/mobileid/shell$ ./mobileid-sign.sh -v 41791234567 'test.com: Do you want to login to corporate VPN?' en
OK with following details and checks:
 1) Transaction ID : AP.TEST.16766.7766 -> same as in request
    MSSP TransID   : HE9dyebu
 2) Signed by      : 41791234567 -> same as in request
 3) Signer         : subject=serialNumber=MIDCHE0LMPAJJ828,CN=MIDCHE0LMPAJJ828:PN
                     issuer=CN=Swisscom Rubin CA 3,OU=Digital Certificate Services,O=Swisscom,C=ch
                     validity= notBefore=Apr  9 12:53:11 2020 GMT notAfter=Apr  9 12:53:11 2023 GMT
 4) Signed Data    : test.com: Do you want to login to corporate VPN? -> Decode and verify: success and same as in request
 5) Status code    : 500 with exit 0
    Status details : SIGNATURE
```

## Known Issues

### curl: Unable to load client cert -8018

curl was compiled with NSS, which you can see by checking the version:

```
$ curl -V
curl 7.19.7 (x86_64-redhat-linux-gnu) libcurl/7.19.7 NSS/3.14.3.0 zlib/1.2.3 libidn/1.18 libssh2/1.4.2
Protocols: tftp ftp telnet dict ldap ldaps http file https ftps scp sftp 
Features: GSS-Negotiate IDN IPv6 Largefile NTLM SSL libz
```

The solution is to provide curl with a reference to the NSS database that stores the client certificate you want to use.

```
mkdir /home/user/nss
certutil -N -d /home/user/nss
pk12util -i /home/user/mobileid-cmd/shell/mykey.p12 -d /home/user/nss
export SSL_DIR=/home/user/nss
certutil -L -d /home/user/nss -n alias
```

Finally, change the curl command to use `--cert alias`

### OS X 10.x: Requests always fail with MSS error 104: _Wrong SSL credentials_.

The `curl` shipped with OS X uses their own Secure Transport engine, which broke the --cert option, see: http://curl.haxx.se/mail/archive-2013-10/0036.html

Install curl from Mac Ports `sudo port install curl` or home-brew: `brew install curl && brew link --force curl`.

