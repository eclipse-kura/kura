package org.eclipse.kura.certificate;

import java.security.cert.Certificate;
import java.util.Enumeration;

import org.eclipse.kura.KuraException;


/**
 * The CertificatesService is used to manage the storage, listing and retrieval of public certificates 
 * from a key store.
 *
 */
public interface CertificatesService {
	
	/**
	 * The storeCertificate interface method receives a certificate and an alias that should be stored in a key store
	 * 
	 * @param cert The certificate of type Certificate that has to be stored in a key store
	 * @param alias A string that will be used to identify the certificate in a key store
	 * @throws KuraException raised if the certificate storage operation failed
	 * 
	 */
	public void storeCertificate(Certificate cert, String alias) throws KuraException;
	
	/**
	 * listStoredCertificatesAliases provides an enumeration of strings representing the different public certificates 
	 * stored in a key store
	 * 
	 * @return An enumeration containing the strings that represent the aliases stored in a key store.
	 * 
	 */
	public Enumeration<String> listStoredCertificatesAliases();
	
	/**
	 * listSSLCertificatesAliases provides an enumeration of strings representing the different ssl certificates 
	 * stored in a key store
	 * 
	 * @return An enumeration containing the strings that represent the aliases stored in a key store.
	 * 
	 */
	public Enumeration<String> listSSLCertificatesAliases();
	
	/**
	 * listDMCertificatesAliases provides an enumeration of strings representing the different certificates used to authenticate 
	 * the messages coming from the remote platform and stored in the device key store
	 * 
	 * @return An enumeration containing the strings that represent the aliases stored in a key store.
	 * 
	 */
	public Enumeration<String> listDMCertificatesAliases();
	
	/**
	 * listBundleCertificatesAliases provides an enumeration of strings representing the different certificates used to sign  
	 * the bundles and that are stored in the device key store
	 * 
	 * @return An enumeration containing the strings that represent the aliases stored in a key store.
	 * 
	 */
	public Enumeration<String> listBundleCertificatesAliases();
	
	/**
	 * returnCertificate returns the certificate corresponding to the specified alias.
	 * 
	 * @param alias The string used to identify the certificate in a key store
	 * @return A Certificate object retrieved from a key store.
	 * 
	 */
	public Certificate returnCertificate(String alias) throws KuraException;
	
	/**
	 * removeCertificate tries to remove the specified certificate from the key store. Returns true, if the removal operation succeeded. False, otherwise.
	 * 
	 * @param alias The string used to identify the certificate in a key store
	 * @throws KuraException raised if the certificate removal operation failed
	 * 
	 */
	public void removeCertificate(String alias) throws KuraException;
	

}
