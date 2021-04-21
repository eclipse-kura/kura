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
package org.eclipse.kura.core.keystore.request.handler.test;

import static org.eclipse.kura.cloudconnection.request.RequestHandlerMessageConstants.ARGS_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;
import javax.ws.rs.WebApplicationException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.core.keystore.request.handler.KeystoreServiceRequestHandlerV1;
import org.eclipse.kura.core.keystore.util.CertificateInfo;
import org.eclipse.kura.core.keystore.util.CsrInfo;
import org.eclipse.kura.core.keystore.util.EntryInfo;
import org.eclipse.kura.core.keystore.util.EntryType;
import org.eclipse.kura.core.keystore.util.KeyPairInfo;
import org.eclipse.kura.core.keystore.util.PrivateKeyInfo;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

public class KeystoreServiceRequestHandlerTest {

    private static final String ENTRIES_RESOURCE = "entries";
    private static final String KEYSTORES_RESOURCE = "keystores";
    private final String EMPTY_KEYSTORE_1 = "[{\"keystoreServicePid\":\"MyKeystore\",\"type\":\"jks\",\"size\":0}]";
    private final String EMPTY_KEYSTORE_2 = "[{\"keystoreServicePid\":\"MyKeystore\",\"type\":\"pkcs12\",\"size\":0}]";
    private final String KEYSTORE_ENTRY = "[{\"subjectDN\":\"CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown\",\"issuer\":\"CN=Unknown,OU=Unknown,O=Unknown,L=Unknown,ST=Unknown,C=Unknown\",\"startDate\":\"Wed, 14 Apr 2021 08:02:28 GMT\",\"expirationDate\":\"Tue, 13 Jul 2021 08:02:28 GMT\",\"algorithm\":\"SHA256withRSA\",\"size\":2048,\"keystoreServicePid\":\"MyKeystore\",\"alias\":\"alias\",\"type\":\"TRUSTED_CERTIFICATE\"}]";
    private final String KEYSTORE_ENTRY_WITH_CERT = "{\"subjectDN\":\"CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown\",\"issuer\":\"CN=Unknown,OU=Unknown,O=Unknown,L=Unknown,ST=Unknown,C=Unknown\",\"startDate\":\"Wed, 14 Apr 2021 08:02:28 GMT\",\"expirationDate\":\"Tue, 13 Jul 2021 08:02:28 GMT\",\"algorithm\":\"SHA256withRSA\",\"size\":2048,\"certificate\":\"-----BEGIN CERTIFICATE-----\\nMIIDdzCCAl+gAwIBAgIEQsO0gDANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV\\nbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD\\nVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du\\nMB4XDTIxMDQxNDA4MDIyOFoXDTIxMDcxMzA4MDIyOFowbDEQMA4GA1UEBhMHVW5r\\nbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE\\nChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCC\\nASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJSWJDxu8UNC4JGOgK31WCvz\\nNKy2ONH+jTVKnBY7Ckb1hljJY0sKO55aG1HNDfkev2lJTsPIz0nJjNsqBvB1flvf\\nr6XVCxdN0yxvU5g9SpRxE/iiPX0Qt7463OfzyKW97haJrrhF005RHYNcORMY/Phj\\nhFDnZhtAwpbQLzq2UuIZ7okJsx0IgRbjH71ZZuvYCqG7Ct/bp1D7w3tT7gTbIKYH\\nppQyG9rJDEh9+cr9Hyk8Gz7aAbPT/wMH+/vXDjH2j/M1Tmed0ajuGCJumaTQ4eHs\\n9xW3B3ugycb6e7Osl/4ESRO5RQL1k2GBONv10OrKDoZ5b66xwSJmC/w3BRWQ1cMC\\nAwEAAaMhMB8wHQYDVR0OBBYEFPospETb5HNeD/DmS9mwt+v/AYq/MA0GCSqGSIb3\\nDQEBCwUAA4IBAQBxMe1xQVQKt36A5qVlEZyxI9eb6eQRlYzorOgP2tFaOsvDPpRI\\nCALhPmxgQl/5QvKFfCXKoxWj1Spg4sF6fJp6jhSjLpmChS9lf5fRaWS20/pxIddM\\n10diq3r6HxLKSxCYK7Pf5scOeZquvwfo8Kxye01bvCMFf1s1K3ZEZszk5Oo2MnWU\\nU22YnXfZm1C0h2WMUcou35A7CeVAHPWI0Rvefojv1qYlQScJOkCN5lO6C/1qvRhq\\nnDQdQN/m1HQbpfh2DD6F33nBjkyLQyMRF8uMnspLrLLj8lecSTJZO4fGJOaIXh3O\\n44da9A02FAf5nRRQpwP2x/4IZ5RTRBzrqbqD\\n-----END CERTIFICATE-----\",\"keystoreServicePid\":\"MyKeystore\",\"alias\":\"alias\",\"type\":\"TRUSTED_CERTIFICATE\"}";
    private final String CERTIFICATE = "-----BEGIN CERTIFICATE-----\n"
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
    private final String JSON_MESSAGE_PUT_CERT = "{\n" + "   \"keystoreServicePid\":\"MyKeystore\",\n"
            + "   \"alias\":\"myCertTest99\",\n" + "   \"type\":\"TrustedCertificate\",\n" + "   \"certificate\":\""
            + this.CERTIFICATE + "\n" + "}";
    private final String JSON_MESSAGE_DEL = "{\n" + "    \"keystoreServicePid\" : \"MyKeystore\",\n"
            + "    \"alias\" : \"mycerttestec\"\n" + "}";
    private final String PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n"
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
    private final String[] CERTIFICATE_CHAIN = {
            "-----BEGIN CERTIFICATE-----\n" + "MIIDdzCCAl+gAwIBAgIED3hXJjANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV\n"
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
                    + "L7w7ELyBzbNlk8a3dQc3Dcg+tu7VAf2tRtmc\n" + "-----END CERTIFICATE-----" };
    private final String JSON_MESSAGE_PUT_KEY = "{\n" + "   \"keystoreServicePid\":\"MyKeystore\",\n"
            + "   \"alias\":\"myPrivateKey\",\n" + "   \"type\":\"PrivateKey\",\n" + "   \"privateKey\" : \""
            + this.PRIVATE_KEY + ",\n" + "   \"certificateChain\":[\"" + this.CERTIFICATE_CHAIN + "\"]\n" + "}";
    private final String JSON_MESSAGE_PUT_KEY_PAIR = "{\n" + "   \"keystoreServicePid\":\"MyKeystore\",\n"
            + "   \"alias\":\"myKeyPair\",\n" + "   \"type\":\"KeyPair\",\n" + "   \"algorithm\" : \"RSA\",\n"
            + "   \"size\": 2048,\n" + "   \"signatureAlgorithm\" : \"SHA256WithRSA\",\n"
            + "   \"attributes\" : \"CN=Kura, OU=IoT, O=Eclipse, C=US\"\n" + "}";
    private final String JSON_MESSAGE_GET_CSR = "{\n" + "    \"keystoreServicePid\":\"MyKeystore\",\n"
            + "    \"alias\":\"alias\",\n" + "    \"algorithm\" : \"SHA256withRSA\",\n"
            + "    \"attributes\" : \"CN=Kura, OU=IoT, O=Eclipse, C=US\"\n" + "}";
    private final String JSON_MESSAGE_GET_KEYS_BY_KEYSTORE = "{\n" + "    \"keystoreServicePid\":\"MyKeystore\"\n"
            + "}";
    private final String JSON_MESSAGE_GET_KEYS_BY_ALIAS = "{\n" + "    \"alias\":\"alias\"\n" + "}";
    private final String JSON_MESSAGE_GET_KEYS_BY_KEYSTORE_ALIAS = "{\n"
            + "    \"keystoreServicePid\":\"MyKeystore\", \n" + "    \"alias\":\"alias\"\n" + "}";

    @Test(expected = KuraException.class)
    public void doGetTestNoResources() throws KuraException {
        KeystoreServiceRequestHandlerV1 handler = new KeystoreServiceRequestHandlerV1();

        List<String> resourcesList = Collections.emptyList();
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doGet(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoGetOtherwise() throws KuraException, NoSuchFieldException {
        KeystoreServiceRequestHandlerV1 handler = new KeystoreServiceRequestHandlerV1();

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("test");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doGet(null, message);
    }

    @Test
    public void testDoGetKeystores() throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
                try {
                    this.certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(KEYSTORES_RESOURCE);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        List<String> responses = Arrays.asList(this.EMPTY_KEYSTORE_1, this.EMPTY_KEYSTORE_2);
        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertTrue(responses.contains(new String(resPayload.getBody(), StandardCharsets.UTF_8)));
    }

    @Test
    public void testDoGetAllKeys() throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        ByteArrayInputStream is = new ByteArrayInputStream(this.CERTIFICATE.getBytes());
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        ks.setCertificateEntry("alias", cert);
        Map<String, Entry> certs = new HashMap<>();
        certs.put("alias", new KeyStore.TrustedCertificateEntry(cert));
        when(ksMock.getKeyStore()).thenReturn(ks);
        when(ksMock.getEntries()).thenReturn(certs);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
                try {
                    this.certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(KEYSTORES_RESOURCE);
        resourcesList.add(ENTRIES_RESOURCE);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();
        String response = new String(resPayload.getBody(), StandardCharsets.UTF_8);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(KEYSTORE_ENTRY, response);
    }

    @Test
    public void testDoGetKeysByKeystore()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        ByteArrayInputStream is = new ByteArrayInputStream(this.CERTIFICATE.getBytes());
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        ks.setCertificateEntry("alias", cert);
        Map<String, Entry> certs = new HashMap<>();
        certs.put("alias", new KeyStore.TrustedCertificateEntry(cert));
        when(ksMock.getKeyStore()).thenReturn(ks);
        when(ksMock.getEntries()).thenReturn(certs);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
                try {
                    this.certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected <T> T unmarshal(String jsonString, Class<T> clazz) {
                EntryInfo entryInfo = new EntryInfo(null, "MyKeystore");
                return (T) entryInfo;
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(KEYSTORES_RESOURCE);
        resourcesList.add(ENTRIES_RESOURCE);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        request.setBody(this.JSON_MESSAGE_GET_KEYS_BY_KEYSTORE.getBytes());
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        String response = new String(resPayload.getBody(), StandardCharsets.UTF_8);
        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals("[" + KEYSTORE_ENTRY_WITH_CERT + "]", response);
    }

    @Test
    public void testDoGetKeyByAlias()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        ByteArrayInputStream is = new ByteArrayInputStream(this.CERTIFICATE.getBytes());
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        ks.setCertificateEntry("alias", cert);
        when(ksMock.getKeyStore()).thenReturn(ks);
        when(ksMock.getEntry("alias")).thenReturn(new KeyStore.TrustedCertificateEntry(cert));

        List<String> aliases = new ArrayList<>();
        aliases.add("alias");
        when(ksMock.getAliases()).thenReturn(aliases);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
                try {
                    this.certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected <T> T unmarshal(String jsonString, Class<T> clazz) {
                EntryInfo entryInfo = new EntryInfo("alias", null);
                return (T) entryInfo;
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(KEYSTORES_RESOURCE);
        resourcesList.add(ENTRIES_RESOURCE);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        request.setBody(this.JSON_MESSAGE_GET_KEYS_BY_ALIAS.getBytes());
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());

        String response = new String(resPayload.getBody(), StandardCharsets.UTF_8);
        assertEquals(this.KEYSTORE_ENTRY_WITH_CERT, response);
    }

    @Test
    public void testDoGetKeyByKeystoreAlias()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        ByteArrayInputStream is = new ByteArrayInputStream(this.CERTIFICATE.getBytes());
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        ks.setCertificateEntry("alias", cert);
        when(ksMock.getKeyStore()).thenReturn(ks);
        when(ksMock.getEntry("alias")).thenReturn(new KeyStore.TrustedCertificateEntry(cert));

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
                try {
                    this.certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected <T> T unmarshal(String jsonString, Class<T> clazz) {
                EntryInfo entryInfo = new EntryInfo("alias", "MyKeystore");
                return (T) entryInfo;
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(KEYSTORES_RESOURCE);
        resourcesList.add(ENTRIES_RESOURCE);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        request.setBody(this.JSON_MESSAGE_GET_KEYS_BY_KEYSTORE_ALIAS.getBytes());
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();
        String response = new String(resPayload.getBody(), StandardCharsets.UTF_8);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(this.KEYSTORE_ENTRY_WITH_CERT, response);
    }

    @Test
    public void testDoPutTrustedCertificate()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
                try {
                    this.certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected <T> T unmarshal(String jsonString, Class<T> clazz) {
                assertEquals(KeystoreServiceRequestHandlerTest.this.JSON_MESSAGE_PUT_CERT, jsonString);
                CertificateInfo info = new CertificateInfo("myCertTest99", "MyKeystore");
                info.setType(EntryType.TRUSTED_CERTIFICATE);
                info.setCertificate(KeystoreServiceRequestHandlerTest.this.CERTIFICATE);
                return (T) info;
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(KEYSTORES_RESOURCE);
        resourcesList.add(ENTRIES_RESOURCE);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        request.setBody(this.JSON_MESSAGE_PUT_CERT.getBytes());
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doPut(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
    }

    @Test(expected = WebApplicationException.class)
    public void testDoPutPrivateKey()
            throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
                try {
                    this.certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected <T> T unmarshal(String jsonString, Class<T> clazz) {
                PrivateKeyInfo info = new PrivateKeyInfo("myPrivateKey", "MyKeystore");
                info.setType(EntryType.PRIVATE_KEY);
                info.setPrivateKey(KeystoreServiceRequestHandlerTest.this.PRIVATE_KEY);
                info.setCertificateChain(KeystoreServiceRequestHandlerTest.this.CERTIFICATE_CHAIN);
                return (T) info;
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(KEYSTORES_RESOURCE);
        resourcesList.add(ENTRIES_RESOURCE);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        request.setBody(this.JSON_MESSAGE_PUT_KEY.getBytes());
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doPut(null, message);
        resMessage.getPayload();
    }

    @Test
    public void testDoPutKeyPair() throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
                try {
                    this.certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected <T> T unmarshal(String jsonString, Class<T> clazz) {
                KeyPairInfo info = new KeyPairInfo("myKeyPair", "MyKeystore");
                info.setType(EntryType.KEY_PAIR);
                info.setAlgorithm("RSA");
                info.setSignatureAlgorithm("SHA256WithRSA");
                info.setAttributes("CN=Kura, OU=IoT, O=Eclipse, C=US");
                info.setSize(2048);
                return (T) info;
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(KEYSTORES_RESOURCE);
        resourcesList.add(ENTRIES_RESOURCE);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        request.setBody(this.JSON_MESSAGE_PUT_KEY_PAIR.getBytes());
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doPut(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
    }

    @Test
    public void getCSRTest() throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);
        when(ksMock.getEntry("alias")).thenReturn(createPrivateKey("alias", this.PRIVATE_KEY, this.CERTIFICATE_CHAIN));
        String csrString = "-----BEGIN CERTIFICATE REQUEST-----";
        when(ksMock.getCSR(any(String.class), any(X500Principal.class), any(String.class))).thenReturn(csrString);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
                try {
                    this.certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected <T> T unmarshal(String jsonString, Class<T> clazz) {
                CsrInfo info = new CsrInfo("alias", "MyKeystore");
                info.setType(EntryType.CSR);
                info.setSignatureAlgorithm("SHA256withRSA");
                info.setAttributes("CN=Kura, OU=IoT, O=Eclipse, C=US");
                return (T) info;
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(KEYSTORES_RESOURCE);
        resourcesList.add(ENTRIES_RESOURCE);
        resourcesList.add("csr");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        request.setBody(this.JSON_MESSAGE_GET_CSR.getBytes());
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        String reds = new String(resPayload.getBody(), StandardCharsets.UTF_8);
        assertTrue(reds.contains(csrString));
    }

    @Test
    public void testDoDel() throws KuraException, NoSuchFieldException, GeneralSecurityException, IOException {
        KeystoreService ksMock = mock(KeystoreService.class);
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "some password".toCharArray();
        ks.load(null, password);
        when(ksMock.getKeyStore()).thenReturn(ks);

        KeystoreServiceRequestHandlerV1 keystoreRH = new KeystoreServiceRequestHandlerV1() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put("MyKeystore", ksMock);
                try {
                    this.certFactory = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                    // Do nothing...
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected <T> T unmarshal(String jsonString, Class<T> clazz) {
                assertEquals(KeystoreServiceRequestHandlerTest.this.JSON_MESSAGE_DEL, jsonString);
                return (T) new EntryInfo("mycerttestec", "MyKeystore");
            }
        };
        keystoreRH.activate(null);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(KEYSTORES_RESOURCE);
        resourcesList.add(ENTRIES_RESOURCE);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        request.setBody(this.JSON_MESSAGE_DEL.getBytes());
        KuraMessage message = new KuraMessage(request, reqResources);

        KuraMessage resMessage = keystoreRH.doDel(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
    }

    private PrivateKeyEntry createPrivateKey(String alias, String privateKey, String[] certificateChain)
            throws IOException, GeneralSecurityException, KuraException {
        // Works with RSA and DSA. EC is not supported since the certificate is encoded
        // with ECDSA while the corresponding private key with EC.
        // This cause an error when the PrivateKeyEntry is generated.
        Certificate[] certs = parsePublicCertificates(certificateChain);

        Security.addProvider(new BouncyCastleProvider());
        PEMParser pemParser = new PEMParser(new StringReader(privateKey));
        Object object = pemParser.readObject();
        pemParser.close();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        PrivateKey privkey = null;
        if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
            privkey = converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
        }

        return new PrivateKeyEntry(privkey, certs);
    }

    private X509Certificate[] parsePublicCertificates(String[] publicKeys) throws CertificateException {
        List<X509Certificate> certificateChain = new ArrayList<>();
        for (String publicKey : publicKeys) {
            ByteArrayInputStream is = new ByteArrayInputStream(publicKey.getBytes());
            certificateChain.add((X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is));
        }
        return certificateChain.toArray(new X509Certificate[0]);
    }
}
