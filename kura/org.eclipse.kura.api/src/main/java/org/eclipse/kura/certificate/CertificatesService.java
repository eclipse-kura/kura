package org.eclipse.kura.certificate;

import java.security.cert.Certificate;
import java.util.Enumeration;

import org.eclipse.kura.KuraException;


/**
 * The CertificatesService is used to manage the storage, listing and retrieval 
 * from a keystore of public certificates.
 *
 */
public interface CertificatesService {
	
	public void storeCertificate(Certificate cert, String alias) throws KuraException;
	
	public Enumeration<String> listPublicCertificatesIDs();
	
	public Certificate returnCertificate(String alias);
	

}
