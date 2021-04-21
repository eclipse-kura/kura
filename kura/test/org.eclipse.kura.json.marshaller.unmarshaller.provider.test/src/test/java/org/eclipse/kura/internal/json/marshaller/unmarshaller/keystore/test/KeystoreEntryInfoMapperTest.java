/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.json.marshaller.unmarshaller.keystore.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.kura.core.keystore.util.CertificateInfo;
import org.eclipse.kura.core.keystore.util.EntryInfo;
import org.eclipse.kura.core.keystore.util.EntryType;
import org.eclipse.kura.core.keystore.util.PrivateKeyInfo;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.keystore.KeystoreEntryInfoMapper;
import org.junit.Test;

public class KeystoreEntryInfoMapperTest {

    @Test
    public void unmarshalTest() {

        String jsonString = "{\n" + "    \"keystoreServicePid\" : \"MyKeystore\",\n" + "    \"alias\" : \"mycerttestec\"\n"
                + "}";
        EntryInfo entry = KeystoreEntryInfoMapper.unmarshal(jsonString);

        assertTrue(entry instanceof EntryInfo);
        assertEquals("mycerttestec", entry.getAlias());
        assertEquals("MyKeystore", entry.getKeystoreServicePid());
        assertNull(entry.getType());
    }

    @Test
    public void unmarshalCertificateTest() {

        String CERTIFICATE = "-----BEGIN CERTIFICATE-----\n"
                + "MIIDdzCCAl+gAwIBAgIEQsO0gDANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV\n"
                + "bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD\n"
                + "VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du\n"
                + "MB4XDTIxMDQxNDA4MDIyOFoXDTIxMDcxMzA4MDIyOFowbDEQMA4GA1UEBhMHVW5r\n"
                + "bm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE\n"
                + "ChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCC\n"
                + "ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJSWJDxu8UNC4JGOgK31WCvz\n"
                + "NKy2ONH+jTVKnBY7Ckb1hljJY0sKO55aG1HNDfkev2lJTsPIz0nJjNsqBvB1flvf\n"
                + "r6XVCxdN0yxvU5g9SpRxE/iiPX0Qt7463OfzyKW97haJrrhF005RHYNcORMY/Phj\n"
                + "hFDnZhtAwpbQLzq2UuIZ7okJsx0IgRbjH71ZZuvYCqG7Ct/bp1D7w3tT7gTbIKYH\n"
                + "ppQyG9rJDEh9+cr9Hyk8Gz7aAbPT/wMH+/vXDjH2j/M1Tmed0ajuGCJumaTQ4eHs\n"
                + "9xW3B3ugycb6e7Osl/4ESRO5RQL1k2GBONv10OrKDoZ5b66xwSJmC/w3BRWQ1cMC\n"
                + "AwEAAaMhMB8wHQYDVR0OBBYEFPospETb5HNeD/DmS9mwt+v/AYq/MA0GCSqGSIb3\n"
                + "DQEBCwUAA4IBAQBxMe1xQVQKt36A5qVlEZyxI9eb6eQRlYzorOgP2tFaOsvDPpRI\n"
                + "CALhPmxgQl/5QvKFfCXKoxWj1Spg4sF6fJp6jhSjLpmChS9lf5fRaWS20/pxIddM\n"
                + "10diq3r6HxLKSxCYK7Pf5scOeZquvwfo8Kxye01bvCMFf1s1K3ZEZszk5Oo2MnWU\n"
                + "U22YnXfZm1C0h2WMUcou35A7CeVAHPWI0Rvefojv1qYlQScJOkCN5lO6C/1qvRhq\n"
                + "nDQdQN/m1HQbpfh2DD6F33nBjkyLQyMRF8uMnspLrLLj8lecSTJZO4fGJOaIXh3O\n"
                + "44da9A02FAf5nRRQpwP2x/4IZ5RTRBzrqbqD\n" + "-----END CERTIFICATE-----";
        String jsonString = "{\n" + "   \"keystoreServicePid\":\"MyKeystore\",\n" + "   \"alias\":\"myCertTest99\",\n"
                + "   \"type\":\"TrustedCertificate\",\n" + "   \"certificate\":\"" + CERTIFICATE + "\"" + "}";
        EntryInfo entry = KeystoreEntryInfoMapper.unmarshal(jsonString);

        assertTrue(entry instanceof CertificateInfo);
        assertEquals("myCertTest99", entry.getAlias());
        assertEquals("MyKeystore", entry.getKeystoreServicePid());
        assertEquals(CERTIFICATE, ((CertificateInfo) entry).getCertificate());
        assertEquals(EntryType.TRUSTED_CERTIFICATE, entry.getType());
    }

    @Test
    public void unmarshalPrivateKeyTest() {

        String PRIVATEKEY = "-----BEGIN PRIVATE KEY-----\n"
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
        String CERTIFICATE_CHAIN = "-----BEGIN CERTIFICATE-----\n"
                + "MIIDdzCCAl+gAwIBAgIED3hXJjANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV\n"
                + "bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD\n"
                + "VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du\n"
                + "MB4XDTIxMDQxMzA4MTQxOVoXDTIxMDcxMjA4MTQxOVowbDEQMA4GA1UEBhMHVW5r\n"
                + "bm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE\n"
                + "ChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCC\n"
                + "ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIJxnU270BVgol2nqBx05vEK\n"
                + "Kh945Vsm/dazX0lVro8ZNKBEQXLlz3v+T5KxIGEcAWnWffvfwTaB3ENq5+0qmZXd\n"
                + "ln3esRBD4NvvU+LIWWsQS1wMT//XgQfaNocEiCHGpaaC9a7R7ylxri7GeY44kA8J\n"
                + "ps1gsuzimdOH3Emf4H4x3P7802P8LsFXndbacByx1WqluYuN8KsNJ3+GB6XwCgBt\n"
                + "2OJEaRgeSC8S7O2PHTWk7aIYCCmRy2KB3EX/KDDA47Fw8BLD/nlmNISAxnHs8zLE\n"
                + "SIgvuOC39yAbrxCGVDed5h8n1x36Au+fWDV/Cwa0anDcyHd2CzoNCSvwcxBbRukC\n"
                + "AwEAAaMhMB8wHQYDVR0OBBYEFCXFCTq9DDNw6jr0nE2VHw+6wqG0MA0GCSqGSIb3\n"
                + "DQEBCwUAA4IBAQAjsKeIU0vf7vaOhUAMV60eP54kr6koiWBjhCxyKXQ+MECTFntn\n"
                + "L459+uTFOCyoytWYjbe9ph79ossTWTCUUPCx9ZSaVdrpK5TyzXI+KBBWqGcLHxqc\n"
                + "1jvU7zKLVf9oKGfhugnFvmj2EqC2vsrQPiG+p1RDfiLI9BqmhoDzBWzjZDdB6xt6\n"
                + "PMAqecHfS24TzyWi8T4gLctcpSN22Aa394ky7sgBJPAQHWe7VWhRB0bVTZntwRsQ\n"
                + "pEraINImKSw+m7MF/75s151yjKOzQxPZufl91oYyQMXoqX2fi0EUWo1oLm1x01dN\n"
                + "L7w7ELyBzbNlk8a3dQc3Dcg+tu7VAf2tRtmc\n" + "-----END CERTIFICATE-----";
        String jsonString = "{\n" + "   \"keystoreServicePid\":\"MyKeystore\",\n" + "   \"alias\":\"myPrivateKey1\",\n"
                + "   \"type\":\"PrivateKey\",\n" + "   \"privateKey\" : \"" + PRIVATEKEY + "\",\n"
                + "   \"certificateChain\":[\"" + CERTIFICATE_CHAIN + "\"]\n" + "}";
        EntryInfo entry = KeystoreEntryInfoMapper.unmarshal(jsonString);

        assertTrue(entry instanceof PrivateKeyInfo);
        assertEquals("myPrivateKey1", entry.getAlias());
        assertEquals("MyKeystore", entry.getKeystoreServicePid());
        assertEquals(CERTIFICATE_CHAIN, ((PrivateKeyInfo) entry).getCertificateChain()[0]);
        assertEquals(PRIVATEKEY, ((PrivateKeyInfo) entry).getPrivateKey());
        assertEquals(EntryType.PRIVATE_KEY, entry.getType());
    }
}
