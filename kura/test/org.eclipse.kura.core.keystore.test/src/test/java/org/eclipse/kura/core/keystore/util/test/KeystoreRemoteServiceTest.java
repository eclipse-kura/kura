package org.eclipse.kura.core.keystore.util.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.CertificateException;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

import org.eclipse.kura.core.keystore.util.KeystoreRemoteService;
import org.junit.Test;

public class KeystoreRemoteServiceTest {

    private final String publicKeyDSA = "-----BEGIN CERTIFICATE-----\n"
            + "MIIEjjCCBDmgAwIBAgIED6yGqzANBglghkgBZQMEAwIFADBJMQswCQYDVQQGEwJm\n"
            + "ZzEKMAgGA1UECBMBZTEKMAgGA1UEBxMBZDEKMAgGA1UEChMBYzEKMAgGA1UECxMB\n"
            + "YjEKMAgGA1UEAxMBYTAeFw0yMTA0MTkxNTAwMjVaFw0yMTA3MTgxNTAwMjVaMEkx\n"
            + "CzAJBgNVBAYTAmZnMQowCAYDVQQIEwFlMQowCAYDVQQHEwFkMQowCAYDVQQKEwFj\n"
            + "MQowCAYDVQQLEwFiMQowCAYDVQQDEwFhMIIDQjCCAjUGByqGSM44BAEwggIoAoIB\n"
            + "AQCPeTXZuarpv6vtiHrPSVG28y7FnjuvNxjo6sSWHz79NgbnQ1GpxBgzObgJ58Ku\n"
            + "HFObp0dbhdARrbi0eYd1SYRpXKwOjxSzNggooi/6JxEKPWKpk0U0CaD+aWxGWPhL\n"
            + "3SCBnDcJoBBXsZWtzQAjPbpUhLYpH51kjviDRIZ3l5zsBLQ0pqwudemYXeI9sCkv\n"
            + "wRGMn/qdgYHnM423krcw17njSVkvaAmYchU5Feo9a4tGU8YzRY+AOzKkwuDycpAl\n"
            + "bk4/ijsIOKHEUOThjBopo33fXqFD3ktm/wSQPtXPFiPhWNSHxgjpfyEc2B3KI8tu\n"
            + "OAdl+CLjQr5ITAV2OTlgHNZnAh0AuvaWpoV499/e5/pnyXfHhe8ysjO65YDAvNVp\n"
            + "XQKCAQAWplxYIEhQcE51AqOXVwQNNNo6NHjBVNTkpcAtJC7gT5bmHkvQkEq9rI83\n"
            + "7rHgnzGC0jyQQ8tkL4gAQWDt+coJsyB2p5wypifyRz6Rh5uixOdEvSCBVEy1W4As\n"
            + "No0fqD7UielOD6BojjJCilx4xHjGjQUntxyaOrsLC+EsRGiWOefTznTbEBplqiuH\n"
            + "9kxoJts+xy9LVZmDS7TtsC98kOmkltOlXVNb6/xF1PYZ9j897buHOSXC8iTgdzEp\n"
            + "baiH7B5HSPh++1/et1SEMWsiMt7lU92vAhErDR8C2jCXMiT+J67ai51LKSLZuovj\n"
            + "ntnhA6Y8UoELxoi34u1DFuHvF9veA4IBBQACggEAbsIJCF7b7OvHijStKh0/8yFd\n"
            + "sxCmqIM2h7jfShrbHaJiyCIjuW1HzuCovFKMM+2yoKu+Hi7FsGxW/bkxZ/aY1KFO\n"
            + "yliphnEvO70l5tKvbJEhIc6Sp1RxC/w8mX1nuzEt0DQdUXsp6Hpi2WB/onvreFZL\n"
            + "+Utq4wYgxWaG+2UvYTHDk4lGPpSoFJI6SosUK8WnxLXgZ13N0ayZA5bsaI8taMOV\n"
            + "ypbmX+iJvsITKnuxXNV2g49Huy2XDSwNn3l1x8hGH8Eici8IXT1l4zo5JDqWmC1E\n"
            + "3YtdEcVbgGnIvOqWdyZbbeIL/qVBAb8yK+V9HrPvx27A9wMnDSJczPq4PwKpOaMh\n"
            + "MB8wHQYDVR0OBBYEFLEqPO2kZodX+JknKTLcZ1uSE+fLMA0GCWCGSAFlAwQDAgUA\n"
            + "A0AAMD0CHGVZgxZ1FdxwlavGNeEi/y6QzgcN11t0JM8biJQCHQCIV3ovl7ZAo8jU\n" + "35V0srUI0ojei0AXCR9+2IH5\n"
            + "-----END CERTIFICATE-----";
    private static String privateKeyDSA = "-----BEGIN PRIVATE KEY-----\n"
            + "MIIBSwIBADCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s5Of2EbdS\n"
            + "PO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVCl\n"
            + "pJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith\n"
            + "1yrv8iIDGZ3RSAHHAhUAl2BQjxUjC8yykrmCouuEC/BYHPUCgYEA9+GghdabPd7L\n"
            + "vKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3\n"
            + "zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImo\n"
            + "g9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoEFgIUaS0Ob0O61UNMULqh2sm52UN4X8g=\n" + "-----END PRIVATE KEY-----";
    private static String publicKeyRSA = "-----BEGIN CERTIFICATE-----\n"
            + "MIICLDCCAZWgAwIBAgIEE9H3JDANBgkqhkiG9w0BAQsFADBJMQswCQYDVQQGEwI1\n"
            + "NjEKMAgGA1UECBMBNTEKMAgGA1UEBxMBNDEKMAgGA1UEChMBMzEKMAgGA1UECxMB\n"
            + "MjEKMAgGA1UEAxMBMTAeFw0yMTA0MTkxNTA2MzZaFw0yMTA3MTgxNTA2MzZaMEkx\n"
            + "CzAJBgNVBAYTAjU2MQowCAYDVQQIEwE1MQowCAYDVQQHEwE0MQowCAYDVQQKEwEz\n"
            + "MQowCAYDVQQLEwEyMQowCAYDVQQDEwExMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCB\n"
            + "iQKBgQCD5T+X1VcCJ7U3UEfFXqmB8RUOhgqvEaZZBa+nf444S0J1HM08AZ3NOOPK\n"
            + "Bj/imA0EDGWSuoB+1MahaCl0zYrxshFTOajngcEfndGs4L0IkBuavGtfMtASyOTb\n"
            + "dKcYGL+OQWaQ/+k7HwDye+TLKJWBMHxxQcvYUF6tyYoUUf43hQIDAQABoyEwHzAd\n"
            + "BgNVHQ4EFgQUxmYO2XPbCCi9OjzFSuJdgcVNgEowDQYJKoZIhvcNAQELBQADgYEA\n"
            + "CETsKRd5/MdrnCqBskN865hdgCLlCxrE1Jw+R9VFThlbfhc4As7Luj5deDO9Usvg\n"
            + "SpVEVljt+AdBm1kp04Ai8PW+UoFJqrfH9jkvGsr2N20eQxomrEnLKXeLlcU/meNf\n"
            + "rV5gD25tKr0nNVToIzjQV5bQmmsZQx5XnAuLzYTsRjw=\n" + "-----END CERTIFICATE-----";
    private static String privateKeyRSA = "-----BEGIN PRIVATE KEY-----\n"
            + "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCCcZ1Nu9AVYKJd\n"
            + "p6gcdObxCiofeOVbJv3Ws19JVa6PGTSgREFy5c97/k+SsSBhHAFp1n3738E2gdxD\n"
            + "auftKpmV3ZZ93rEQQ+Db71PiyFlrEEtcDE//14EH2jaHBIghxqWmgvWu0e8pca4u\n"
            + "xnmOOJAPCabNYLLs4pnTh9xJn+B+Mdz+/NNj/C7BV53W2nAcsdVqpbmLjfCrDSd/\n"
            + "hgel8AoAbdjiRGkYHkgvEuztjx01pO2iGAgpkctigdxF/ygwwOOxcPASw/55ZjSE\n"
            + "gMZx7PMyxEiIL7jgt/cgG68QhlQ3neYfJ9cd+gLvn1g1fwsGtGpw3Mh3dgs6DQkr\n"
            + "8HMQW0bpAgMBAAECggEAL2IR3/C/L2TA1gBWwq98TEaC8pe5yJirUFgr3rmvBPAE\n"
            + "+8qPc6si6UmBoimRN3Uy1j1B2kJ3LtORLTQiNzZoP9YUGnjQHLZrcbjH4fMg+BEd\n"
            + "LrySOr8PccjEUdtFj+9WsNuVXwGHPKi8uuUBtrW5Lp006BmeJQpTElGhpWTb6Tqy\n"
            + "OcNceNz+oP1N3AZ3Mnf6Aq6GuvD4QeGMVEiosHPxMqY0eddNK672zq3A0o1NPA9z\n"
            + "yaVt9UK7SZ6/yOKYrAAM/2iLHmNbrYRer567hgg2B1LGJCRSdB/c34u6lTnPo+Ai\n"
            + "olQahQNFhiJKTXr2kK9WgS6tWXhqUqsVLUCug4mDAQKBgQDKLbhidAYClyOKJW7U\n"
            + "GsbM5dmQV80NsnsN3qphKZDTEMinhs3N4oknhi+mZPeyge4MY73BiHXhnMcUkfhw\n"
            + "nbhRNhrkZTt18S6rTzEp/vDDx+ZosxRKYXmPyuDDWDmvG6ocRyOLIgoYhT0kPQ7a\n"
            + "oXQkpFPjBq1UmqNOcUwEpNG2sQKBgQClKzyAopsjWNptBQPo/j/PN24uThpdd3yX\n"
            + "NenmLZsYJyloKDbOGzEXpuyzdtNAiIVDsQ8RzN5lkF6xVvXnOlSA2gmEc8KJmRl8\n"
            + "/gWzPRHHHNR7QUiGmg9QThrUp2l6DiAm/IcuL0btj99kQa2XcLGlTohwWpdVySSx\n"
            + "abDX7pSRuQKBgH/jy+77VZHt6R1J8IFbLsYN30HfSGaRsCVl5IDxuhrJUyQlsam6\n"
            + "0uediibHV6gjaGGN9kql92tvsL7iVzVlj2JPx1MSdjp1BgB3Z7IZAlPV73nrTbp/\n"
            + "TlYXD3aCKHsMFN8uYN1x+tDn93Uk6nCCEOXczPOfFaWe7A6CvINzfvUBAoGAUJEm\n"
            + "khi/VB6jbUpk/eIHfiyrsiqm8bC3NYs27PCSFtYDfKshEKhy6faiv2fW5EOzvbFA\n"
            + "iI5GbYRerGKe0IvDbJbuzY0p97SWmkHOxf+kDFwjyXuuxPmhPqraq6B98uuxA1Nr\n"
            + "HTwyfO8RKPZglt6ByQDlzOhjqZTUMTY87ReToQECgYBKdX3Idr4zvODkXIlG852C\n"
            + "o135W+7AWr+dYRLx1FcvgMU9SbF9cwUU5Zutbrv+Kl8xGPyfx09MJ6BNxTkkr09J\n"
            + "BpbrbOZsUDjMjojyQYL4Ll9rLohk+Pq73xXJjtTRIXZVXJg27pEEqzcVB4o9vgli\n" + "yzOqhyTKM9JP7Uda6Fv6DA==\n"
            + "-----END PRIVATE KEY-----";
    private static String publicKeyEC = "-----BEGIN CERTIFICATE-----\n"
            + "MIIBqjCCAU2gAwIBAgIELVNe7DAMBggqhkjOPQQDAgUAMEkxCzAJBgNVBAYTAnRy\n"
            + "MQowCAYDVQQIEwE1MQowCAYDVQQHEwE0MQowCAYDVQQKEwEzMQowCAYDVQQLEwEy\n"
            + "MQowCAYDVQQDEwExMB4XDTIxMDQxOTE1MDM0M1oXDTIxMDcxODE1MDM0M1owSTEL\n"
            + "MAkGA1UEBhMCdHIxCjAIBgNVBAgTATUxCjAIBgNVBAcTATQxCjAIBgNVBAoTATMx\n"
            + "CjAIBgNVBAsTATIxCjAIBgNVBAMTATEwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNC\n"
            + "AARdxv7a46GcCcGrbbl9EolhfvKPJN4q8vfX8soAMFQ/7AOuKkz/2voFJ5kMK+gK\n"
            + "31kbILrPnZa8IFmOB0d6qwLToyEwHzAdBgNVHQ4EFgQU0K3b/8VqxtBxW2dD81CB\n"
            + "x9PyGUcwDAYIKoZIzj0EAwIFAANJADBGAiEAwN0V4/m/MAtEModdE27xB2bMC13i\n"
            + "7KN7y088ENIykpICIQCjE9XItmBa405Bki9xe+n7T0eFhoCzAPk6lceZLzeFTQ==\n" + "-----END CERTIFICATE-----";

    @Test
    public void createCertificateDSAEntryTest() throws CertificateException {
        TrustedCertificateEntry entry = KeystoreRemoteService.createCertificateEntry(publicKeyDSA);

        assertNotNull(entry);
        assertEquals("DSA", entry.getTrustedCertificate().getPublicKey().getAlgorithm());
        assertEquals(2048,
                ((DSAPublicKey) entry.getTrustedCertificate().getPublicKey()).getParams().getP().bitLength());
        assertEquals("X.509", entry.getTrustedCertificate().getType());

    }

    @Test
    public void createCertificateRSAEntryTest() throws CertificateException {
        TrustedCertificateEntry entry = KeystoreRemoteService.createCertificateEntry(publicKeyRSA);

        assertNotNull(entry);
        assertEquals("RSA", entry.getTrustedCertificate().getPublicKey().getAlgorithm());
        assertEquals(1024, ((RSAPublicKey) entry.getTrustedCertificate().getPublicKey()).getModulus().bitLength());
        assertEquals("X.509", entry.getTrustedCertificate().getType());

    }

    @Test
    public void createCertificateECEntryTest() throws CertificateException {
        TrustedCertificateEntry entry = KeystoreRemoteService.createCertificateEntry(publicKeyEC);

        assertNotNull(entry);
        assertEquals("EC", entry.getTrustedCertificate().getPublicKey().getAlgorithm());
        assertEquals(256,
                ((ECPublicKey) entry.getTrustedCertificate().getPublicKey()).getParams().getOrder().bitLength());
        assertEquals("X.509", entry.getTrustedCertificate().getType());

    }

    @Test
    public void createPrivateKeyDSAEntryTest() throws IOException, GeneralSecurityException {
        PrivateKeyEntry entry = KeystoreRemoteService.createPrivateKey(privateKeyDSA, publicKeyDSA);

        assertNotNull(entry);
        assertEquals("DSA", entry.getCertificate().getPublicKey().getAlgorithm());
        assertEquals(2048, ((DSAPublicKey) entry.getCertificate().getPublicKey()).getParams().getP().bitLength());
        assertEquals("X.509", entry.getCertificate().getType());
        assertEquals("DSA", entry.getPrivateKey().getAlgorithm());
    }

    @Test
    public void createPrivateKeyRSAEntryTest() throws IOException, GeneralSecurityException {
        PrivateKeyEntry entry = KeystoreRemoteService.createPrivateKey(privateKeyRSA, publicKeyRSA);

        assertNotNull(entry);
        assertEquals("RSA", entry.getCertificate().getPublicKey().getAlgorithm());
        assertEquals(1024, ((RSAPublicKey) entry.getCertificate().getPublicKey()).getModulus().bitLength());
        assertEquals("X.509", entry.getCertificate().getType());
        assertEquals("RSA", entry.getPrivateKey().getAlgorithm());
    }
}
