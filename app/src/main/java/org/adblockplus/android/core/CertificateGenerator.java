package org.adblockplus.android.core;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import org.adblockplus.android.R;
import org.adblockplus.android.Utils;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.oiw.OIWObjectIdentifiers;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.*;
import org.spongycastle.cert.X509ExtensionUtils;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openssl.PEMKeyPair;
import org.spongycastle.openssl.PEMParser;
import org.spongycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.spongycastle.openssl.jcajce.JcaPEMWriter;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.operator.bc.BcDigestCalculatorProvider;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import sunlabs.brazil.util.Base64;

import java.io.*;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CertificateGenerator {
    static {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
    }

    private static final String TAG = Utils.getTag(CertificateGenerator.class);
    private static File certDir = null;
    private static X509Certificate caCert = null;
    private static PrivateKey caPri = null;

    public static class CertFile {
        public final File publicFile, privateFile, dir;

        private CertFile(File certDir, File pub, File pri) {
            this.publicFile = pub;
            this.privateFile = pri;
            dir = certDir;
        }

        public CertFile(File certDir) {
            this(certDir, new File(certDir, "public.pem"), new File(certDir, "private.pem"));
        }

        public boolean exists() {
            return privateFile.exists() && publicFile.exists();
        }
    }

    private static void initCa(Context context) {
        if (caCert != null && caPri != null) return;

        InputStream key = context.getResources().openRawResource(R.raw.ca_key);
        InputStream cert = context.getResources().openRawResource(R.raw.ca_cert);
        CertificateFactory fact = null;
        try {
            fact = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        X509Certificate cer = null;
        try {
            caCert = (X509Certificate) fact.generateCertificate(cert);
        } catch (CertificateException e) {
            Log.e(TAG, e.getMessage(), e);
        }


        try {
            PEMParser pp = new PEMParser(new BufferedReader(new InputStreamReader(key)));
            PEMKeyPair pemKeyPair = (PEMKeyPair) pp.readObject();
            KeyPair kp = new JcaPEMKeyConverter().getKeyPair(pemKeyPair);
            pp.close();
            caPri = kp.getPrivate();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Nullable
    private static String getEncodedDomain(final String domain) {
        try {
            return URLEncoder.encode(domain, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    private static File getCertDir(final Context context, final String domain) {
        if (certDir == null) {
            certDir = new File(context.getFilesDir(), "certs");
        }
        File dir = new File(certDir, getEncodedDomain(domain));
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dir;
    }

    public static CertFile getCert(final Context context, final String domain) {
        File certDir = getCertDir(context, domain);
        CertFile certFile = new CertFile(certDir);

        if (certFile.exists()) {
            return certFile;
        }
        final int KEY_LENGTH = 4096;

        initCa(context);

        // GENERATE THE PUBLIC/PRIVATE RSA KEY PAIR
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {

            Log.e(TAG, e.getMessage(), e);
            return null;
        }
        keyPairGenerator.initialize(KEY_LENGTH, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        try {
            X509Certificate clientCert = generateV3Certificate(
                    keyPair.getPublic(),
                    caPri, caCert,
                    domain
            );

            String certContent = certToString(clientCert);
            String privateKeyContent = privateKeyToString(keyPair.getPrivate());

            writeToFile(certFile.publicFile, certContent);
            writeToFile(certFile.privateFile, privateKeyContent);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }

    private static String certToString(X509Certificate cert) {
        final StringWriter writer = new StringWriter();
        final JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        try {
            pemWriter.writeObject(cert);
            pemWriter.flush();
            pemWriter.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return writer.toString();
    }

    private static void writeToFile(File file, String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file));
            outputStreamWriter.write(data);
            outputStreamWriter.close();

            Log.d(TAG, String.format("Written file: %s", file.getAbsolutePath()));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private static String privateKeyToString(PrivateKey privateKey) {
        StringWriter writer = new StringWriter();
        writer.write("-----BEGIN RSA PRIVATE KEY-----\n");
        writer.write(Base64.encode(privateKey.getEncoded()));
        writer.write("\n-----END RSA PRIVATE KEY-----\n");
        return writer.toString();
    }

    private static X509Certificate generateV3Certificate(
            final PublicKey publicKey,
            final PrivateKey caPrivateKey, final X509Certificate caCert,
            final String subject
    ) throws Exception {
        try {
            X500Name subjectDN = new X500Name("CN=" + subject);

            // Serial Number
            BigInteger serialNumber = new BigInteger(52 * 4, new SecureRandom());

            // Validity

            final int VALIDITY_IN_DAYS = 90;
            Calendar startDate = Calendar.getInstance();
            Calendar endDate = Calendar.getInstance();
            startDate.setTime(new Date());
            endDate.setTime(new Date());
            endDate.add(Calendar.DAY_OF_YEAR, VALIDITY_IN_DAYS);

            // SubjectPublicKeyInfo
            SubjectPublicKeyInfo subjPubKeyInfo = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(publicKey
                    .getEncoded()));

            X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(new X500Name(caCert.getSubjectDN().getName()),
                    serialNumber, startDate.getTime(), endDate.getTime(), subjectDN, subjPubKeyInfo);

            DigestCalculator digCalc = new BcDigestCalculatorProvider()
                    .get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));
            X509ExtensionUtils x509ExtensionUtils = new X509ExtensionUtils(digCalc);

            // Subject Key Identifier
            certGen.addExtension(Extension.subjectKeyIdentifier, false,
                    x509ExtensionUtils.createSubjectKeyIdentifier(subjPubKeyInfo));

            // Authority Key Identifier
            certGen.addExtension(Extension.authorityKeyIdentifier, false,
                    x509ExtensionUtils.createAuthorityKeyIdentifier(subjPubKeyInfo));

            // Key Usage
            certGen.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

            // Extended Key Usage
            KeyPurposeId[] EKU = {KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth};

            certGen.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(EKU));

            // Basic Constraints
            certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));


            List<GeneralName> sanNameList = new ArrayList<>();

            sanNameList.add(new GeneralName(GeneralName.dNSName, subject));
            GeneralName[] sanNames = sanNameList.toArray(new GeneralName[sanNameList.size()]);
            certGen.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(sanNames));


            // Content Signer
            ContentSigner sigGen = new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(caPrivateKey);

            // Certificate
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certGen.build(sigGen));
        } catch (Exception e) {
            throw new RuntimeException("Error creating X509v3Certificate.", e);
        }
    }
}
