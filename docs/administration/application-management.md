# Application Management

## Package Installation

After developing your application and generating a deployment package that contains the bundles to be deployed (refer to the Development section for more information), you may install it on the gateway using the **Packages** option in the **System** area of the Kura Gateway Administration Console as shown below.

![](images/packageInstall.png)

Upon a successful installation, the new component appears in the Services list (shown as the _Heater_ example in these screen captures). Its configuration may be modified according to the defined parameters as shown the _Heater_ display that follows.

![](images/packageConfig.png)

## Eclipse Kura Marketplace

Kura allows the installation and update of running applications via the Eclipse Kura Marketplace.
The **Packages** page has, in the top part of the page a section dedicated to the Eclipse Kura Marketplace.

<figure markdown>
  ![](images/marketplaceInstall.png){ style="border-radius: 7px;"}
  <figcaption></figcaption>
</figure>

Dragging an application reference taken from the Eclipse Kura Marketplace to the specific area of the Kura Web Administrative Console will instruct Kura to download and install the corresponding package, as seen below:

![](images/packageMarketplace.png)

!!! warning
    If the installation from the Eclipse Marketplace fails, it can be for the lack of the correct certificates. In this case, import the following certificate in the _SSLKeystore_ from the _Certificate List_ tab under the _Security_ section. For more details about the procedure see [here](../../gateway-configuration/keys-and-certificates/).

    ```
    -----BEGIN CERTIFICATE-----
    MIIHxTCCBq2gAwIBAgIQC3JNX7K6UFPge2A+oFnmdjANBgkqhkiG9w0BAQsFADBP
    MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMSkwJwYDVQQDEyBE
    aWdpQ2VydCBUTFMgUlNBIFNIQTI1NiAyMDIwIENBMTAeFw0yMjExMDkwMDAwMDBa
    Fw0yMzEyMTAyMzU5NTlaMG8xCzAJBgNVBAYTAkNBMRAwDgYDVQQIEwdPbnRhcmlv
    MQ8wDQYDVQQHEwZPdHRhd2ExJTAjBgNVBAoTHEVjbGlwc2Uub3JnIEZvdW5kYXRp
    b24sIEluYy4xFjAUBgNVBAMMDSouZWNsaXBzZS5vcmcwggIiMA0GCSqGSIb3DQEB
    AQUAA4ICDwAwggIKAoICAQC5hXH2cQoOQlXs5cQ5itZ1Dzct9R+bqr2HaF+imlgo
    xJ+Vw1ukfQPpSbmSO17A0hLgpSJyVgoPlpOKkg6LGTz8/2qB7DWHdQbg2p0IGQhr
    dm4oJN2qknnGNl/YYkjz2QJswr1M98raydmq0hqJi0M3q9JSO64O3wOMNduvNG+O
    rCBol7cbxLr7NNoFxZncZ9giP7QF0XYS6nA8dtIyXU3SARRSPn6y9OX1ttltveck
    41ocaU8ORiTF7i89t649XAbtsvxUWM+qVnvlMxpaXqbhnrXMQ/pV2yfdU/qiFQth
    +RqFgBYoX5roxvmjB14+2qlymn236N4KOGhvfr+Fp8C8Fv6N6wFyKZctXewQ6IsA
    3zDvJmF3QaCz6h88lg+IqbRjX5MOjhSkE7XDNKb+xAw5pYzkn9LP+QJLf0iYJw2D
    Z/X+InVPiZ5UdXyXWypN3q0W5vlz/TmWuVZv76/azZ3anoSPiKh+F3si1xZVEMZQ
    IkqsgUfq69M4KvHrdi4nGEOfdBHxjos9ul1AsJR57hrhIchsESthUK04e7d2LYOB
    hHAr0uJNdwFsFD2EBR25ogN83bZ8NaDrrdK2P6sV+hWWK+MY1qRuRub7/fYuR4AU
    82toms9p1usjuyMmuIGEpLwk7jZe6XITcbXQEXDA8JKSZrZ/mOA4yTfIGR/gXXB7
    wQIDAQABo4IDezCCA3cwHwYDVR0jBBgwFoAUt2ui6qiqhIx56rTaD5iyxZV2ufQw
    HQYDVR0OBBYEFO8gL5LNWmSgCqbujR1qH0bUfrIrMCUGA1UdEQQeMByCDSouZWNs
    aXBzZS5vcmeCC2VjbGlwc2Uub3JnMA4GA1UdDwEB/wQEAwIFoDAdBgNVHSUEFjAU
    BggrBgEFBQcDAQYIKwYBBQUHAwIwgY8GA1UdHwSBhzCBhDBAoD6gPIY6aHR0cDov
    L2NybDMuZGlnaWNlcnQuY29tL0RpZ2lDZXJ0VExTUlNBU0hBMjU2MjAyMENBMS00
    LmNybDBAoD6gPIY6aHR0cDovL2NybDQuZGlnaWNlcnQuY29tL0RpZ2lDZXJ0VExT
    UlNBU0hBMjU2MjAyMENBMS00LmNybDA+BgNVHSAENzA1MDMGBmeBDAECAjApMCcG
    CCsGAQUFBwIBFhtodHRwOi8vd3d3LmRpZ2ljZXJ0LmNvbS9DUFMwfwYIKwYBBQUH
    AQEEczBxMCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC5kaWdpY2VydC5jb20wSQYI
    KwYBBQUHMAKGPWh0dHA6Ly9jYWNlcnRzLmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydFRM
    U1JTQVNIQTI1NjIwMjBDQTEtMS5jcnQwCQYDVR0TBAIwADCCAX8GCisGAQQB1nkC
    BAIEggFvBIIBawFpAHcA6D7Q2j71BjUy51covIlryQPTy9ERa+zraeF3fW0GvW4A
    AAGEXT11NQAABAMASDBGAiEAhcCrw89ikyhqDWv+ITPVSIarKOLMkbXVT7meDkj9
    fAwCIQDhOyDAtgdvBnICfxqD0InTnc7lKxkgeOjqylPdblAt4wB1ALNzdwfhhFD4
    Y4bWBancEQlKeS2xZwwLh9zwAw55NqWaAAABhF09dR8AAAQDAEYwRAIgOQWh1vJ8
    luRpIVG/t5BOxVoOXd8Y1TOjqjbQ5KaUmJQCIEC2Hpaid4+qoOx9F3tLXEtbu234
    hf6SsMwc/PUiBDb9AHcAtz77JN+cTbp18jnFulj0bF38Qs96nzXEnh0JgSXttJkA
    AAGEXT107wAABAMASDBGAiEA63aWYNorKwqH6TygcjrK3jdljMXf7eZfC+QAHnid
    W0gCIQCnneMrOFjl5v8eTXqDR8PCuOJTr2+1CzYGYr3cDvuZNjANBgkqhkiG9w0B
    AQsFAAOCAQEAkqKtfmiiHsJSlENpyEXCxYUbRi3wDFADhBTw+oItGr24r9YNhatp
    5o+yEDZ3la8vYL5IJd4WSUMZKdPsU+zA6TuMWTjJgRO8jn4Pqye5w5q1XVvXZsvk
    zn+2yHnOfNwFh4yuiy1h7gKjCdI3nUkw/YIA2NtT5Ap58iBJa0py2q3woMalSZZl
    mn/ja9/t8kO2nSFBkFe2HZWWUEp4tOvL9ByQz/5PcpYvFTp54GdpYT/+KEK/zYtG
    27xpfZdJ4icIb/HnCAH77fDLHks/qbK1a0ktUBtrfYRkbUN4ESej3MiKqqgpC2z7
    NDsupck3+/l202BzMqgBliCbJmateCFiWw==
    -----END CERTIFICATE-----

    Common Name: *.eclipse.org
    Subject Alternative Names: *.eclipse.org, eclipse.org
    Organization: Eclipse.org Foundation, Inc.
    Locality: Ottawa
    State: Ontario
    Country: CA
    Valid From: November 8, 2022
    Valid To: December 10, 2023
    Issuer: DigiCert TLS RSA SHA256 2020 CA1, DigiCert Inc
    Serial Number: 0b724d5fb2ba5053e07b603ea059e676
    ```
    
    If the dp is not hosted by Eclipse, the download will likely fail. In this case, retrieve 
    the certificate with this command:
    ```
    openssl s_client -showcerts -connect <download_link>:443
    ```
    
    and import in the _SSLKeystore_.

## Package Signature

Once the selected application deployment package (dp) file is installed, it will be listed in the **Packages** page and detailed with the name of the deployment package, the version and the signature status.
The value of the signature field can be **true** if all the bundles contained in the deployment package are digitally signed, or **false** if at least one of the bundles is not signed.

<figure markdown>
  ![](images/dpsignature.png){ style="border-radius: 7px;"}
  <figcaption></figcaption>
</figure>
