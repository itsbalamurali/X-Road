/**
 * The MIT License
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ee.ria.xroad.signer.tokenmanager.token;

import ee.ria.xroad.common.CodedException;
import ee.ria.xroad.common.SystemProperties;
import ee.ria.xroad.common.util.CryptoUtils;
import ee.ria.xroad.common.util.PasswordStore;
import ee.ria.xroad.common.util.TokenPinPolicy;
import ee.ria.xroad.signer.protocol.dto.KeyInfo;
import ee.ria.xroad.signer.protocol.dto.TokenInfo;
import ee.ria.xroad.signer.protocol.dto.TokenStatusInfo;
import ee.ria.xroad.signer.protocol.message.ActivateToken;
import ee.ria.xroad.signer.protocol.message.GenerateKey;
import ee.ria.xroad.signer.protocol.message.InitSoftwareToken;
import ee.ria.xroad.signer.protocol.message.UpdateSoftwareTokenPin;
import ee.ria.xroad.signer.tokenmanager.TokenManager;
import ee.ria.xroad.signer.util.SignerUtil;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;

import static ee.ria.xroad.common.ErrorCodes.X_INTERNAL_ERROR;
import static ee.ria.xroad.common.ErrorCodes.X_PIN_INCORRECT;
import static ee.ria.xroad.common.ErrorCodes.X_TOKEN_PIN_POLICY_FAILURE;
import static ee.ria.xroad.common.ErrorCodes.X_UNSUPPORTED_SIGN_ALGORITHM;
import static ee.ria.xroad.common.util.CryptoUtils.encodeBase64;
import static ee.ria.xroad.common.util.CryptoUtils.loadPkcs12KeyStore;
import static ee.ria.xroad.signer.tokenmanager.TokenManager.addKey;
import static ee.ria.xroad.signer.tokenmanager.TokenManager.isKeyAvailable;
import static ee.ria.xroad.signer.tokenmanager.TokenManager.isTokenActive;
import static ee.ria.xroad.signer.tokenmanager.TokenManager.listKeys;
import static ee.ria.xroad.signer.tokenmanager.TokenManager.setKeyAvailable;
import static ee.ria.xroad.signer.tokenmanager.TokenManager.setTokenActive;
import static ee.ria.xroad.signer.tokenmanager.TokenManager.setTokenAvailable;
import static ee.ria.xroad.signer.tokenmanager.TokenManager.setTokenStatus;
import static ee.ria.xroad.signer.tokenmanager.token.SoftwareTokenUtil.PIN_ALIAS;
import static ee.ria.xroad.signer.tokenmanager.token.SoftwareTokenUtil.PIN_FILE;
import static ee.ria.xroad.signer.tokenmanager.token.SoftwareTokenUtil.createKeyStore;
import static ee.ria.xroad.signer.tokenmanager.token.SoftwareTokenUtil.createTempKeyDir;
import static ee.ria.xroad.signer.tokenmanager.token.SoftwareTokenUtil.generateKeyPair;
import static ee.ria.xroad.signer.tokenmanager.token.SoftwareTokenUtil.getKeyDir;
import static ee.ria.xroad.signer.tokenmanager.token.SoftwareTokenUtil.getKeyStoreFileName;
import static ee.ria.xroad.signer.tokenmanager.token.SoftwareTokenUtil.getTempKeyStoreFileName;
import static ee.ria.xroad.signer.tokenmanager.token.SoftwareTokenUtil.isTokenInitialized;
import static ee.ria.xroad.signer.tokenmanager.token.SoftwareTokenUtil.listKeysOnDisk;
import static ee.ria.xroad.signer.tokenmanager.token.SoftwareTokenUtil.loadCertificate;
import static ee.ria.xroad.signer.util.ExceptionHelper.keyNotAvailable;
import static ee.ria.xroad.signer.util.ExceptionHelper.keyNotFound;
import static ee.ria.xroad.signer.util.ExceptionHelper.tokenNotActive;
import static ee.ria.xroad.signer.util.ExceptionHelper.tokenNotInitialized;

/**
 * Encapsulates the software token worker which handles software signing and key
 * management.
 */
@Slf4j
public class SoftwareTokenWorker extends AbstractTokenWorker {

    // Use no digesting algorithm, since the input data is already a digest
    private static final String SIGNATURE_ALGORITHM = "NONEwithRSA";

    private final Map<String, PrivateKey> privateKeys = new HashMap<>();

    /**
     * Creates new worker.
     *
     * @param tokenInfo the token info
     * @param ignored token type (not used)
     */
    public SoftwareTokenWorker(TokenInfo tokenInfo, SoftwareTokenType ignored) {
        super(tokenInfo);
    }

    @Override
    protected void onUpdate() throws Exception {
        log.trace("onUpdate()");

        updateStatus();

        try {
            findKeysNotInConf();

            if (isPinStored()) {
                updateKeys();
            }
        } catch (Exception e) {
            log.error("Failed to add keys not in conf", e);
        }
    }

    @Override
    protected void onMessage(Object message) throws Exception {
        if (message instanceof InitSoftwareToken) {
            initializeToken(((InitSoftwareToken) message).getPin());
            sendSuccessResponse();
        } else if (message instanceof UpdateSoftwareTokenPin) {
            UpdateSoftwareTokenPin updateTokenPinMessage = (UpdateSoftwareTokenPin) message;
            handleUpdateTokenPin(updateTokenPinMessage.getOldPin(), updateTokenPinMessage.getNewPin());
            sendSuccessResponse();
        } else {
            super.onMessage(message);
        }
    }

    @Override
    protected void activateToken(ActivateToken message) throws Exception {
        if (message.isActivate()) {
            activateToken();
        } else {
            deactivateToken();
        }

        setTokenStatus(tokenId, TokenStatusInfo.OK);
    }

    @Override
    protected GenerateKeyResult generateKey(GenerateKey message)
            throws Exception {
        log.trace("generateKeys()");

        if (!isTokenActive(tokenId)) {
            throw tokenNotActive(tokenId);
        }

        java.security.KeyPair keyPair = generateKeyPair(SystemProperties.getSignerKeyLength());

        String keyId = SignerUtil.randomId();
        savePkcs12Keystore(keyPair, keyId, getKeyStoreFileName(keyId), getPin());

        String publicKeyBase64 = encodeBase64(keyPair.getPublic().getEncoded());

        return new GenerateKeyResult(keyId, publicKeyBase64);
    }

    @Override
    protected void deleteKey(String keyId) throws Exception {
        log.trace("deleteKey({})", keyId);

        if (!isTokenActive(tokenId)) {
            throw tokenNotActive(tokenId);
        }

        Path path = Paths.get(getKeyStoreFileName(keyId));

        log.info("Deleting key file {}", path);

        Files.deleteIfExists(path);
    }

    @Override
    protected void deleteCert(String certId) throws Exception {
        TokenManager.removeCert(certId);
    }

    @Override
    protected byte[] sign(String keyId, String signatureAlgorithmId, byte[] data) throws Exception {
        log.trace("sign({}, {})", keyId, signatureAlgorithmId);

        checkSignatureAlgorithm(signatureAlgorithmId);

        if (!isTokenActive(tokenId)) {
            throw tokenNotActive(tokenId);
        }

        if (!isKeyAvailable(keyId)) {
            throw keyNotAvailable(keyId);
        }

        PrivateKey key = getPrivateKey(keyId);

        if (key == null) {
            throw keyNotFound(keyId);
        }

        log.debug("Signing with key '{}' and signature algorithm '{}'", keyId, signatureAlgorithmId);

        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(key);
        signature.update(data);

        return signature.sign();
    }

    private static void checkSignatureAlgorithm(String signatureAlgorithmId) throws CodedException {
        switch (signatureAlgorithmId) {
            case CryptoUtils.SHA1WITHRSA_ID:
            case CryptoUtils.SHA256WITHRSA_ID:
            case CryptoUtils.SHA384WITHRSA_ID:
            case CryptoUtils.SHA512WITHRSA_ID:
                break;
            default:
                throw CodedException.tr(X_UNSUPPORTED_SIGN_ALGORITHM, "unsupported_sign_algorithm",
                        "Unsupported signature algorithm '%s'", signatureAlgorithmId);
        }
    }

    // ------------------------------------------------------------------------

    private void updateStatus() {
        boolean isInitialized = isTokenInitialized();

        if (!isInitialized) {
            setTokenStatus(tokenId, TokenStatusInfo.NOT_INITIALIZED);
        }

        boolean isActive = isInitialized && isPinStored();
        setTokenActive(tokenId, isActive);

        if (isActive) {
            try {
                activateToken();
            } catch (Exception e) {
                setTokenActive(tokenId, false);

                log.trace("Failed to activate token", e);
            }
        }
    }

    private void updateKeys() throws Exception {
        for (KeyInfo keyInfo : listKeys(tokenId)) {
            String keyId = keyInfo.getId();

            setKeyAvailable(keyId, true);

            if (privateKeys.containsKey(keyId)) {
                continue;
            }

            try {
                initializePrivateKey(keyId);
            } catch (Exception e) {
                setKeyAvailable(keyId, false);

                log.trace("Failed to load private key from key store", e);
            }
        }
    }

    private void findKeysNotInConf() throws Exception {
        log.debug("Searching for new software keys from '{}'", getKeyDir());

        for (String keyId : listKeysOnDisk()) {
            if (!hasKey(keyId)) {
                try {
                    String publicKeyBase64 = isPinStored() ? loadPublicKeyBase64(keyId) : null;

                    log.debug("Found new key with id '{}'", keyId);

                    addKey(tokenId, keyId, publicKeyBase64);
                } catch (Exception e) {
                    log.error("Failed to read pkcs#12 key '{}'", keyId, e);
                }
            }
        }
    }

    private PrivateKey getPrivateKey(String keyId) throws Exception {
        PrivateKey pkey = privateKeys.get(keyId);

        if (pkey == null) {
            initializePrivateKey(keyId);
        }

        return privateKeys.get(keyId);
    }

    private void initializePrivateKey(String keyId) throws Exception {
        PrivateKey pkey = loadPrivateKey(keyId);

        if (pkey != null) {
            log.debug("Found usable key '{}'", keyId);

            privateKeys.put(keyId, pkey);
        }
    }

    private void initializeToken(char[] pin) throws Exception {
        verifyPinProvided(pin);

        log.info("Initializing software token with new pin...");

        if (SystemProperties.shouldEnforceTokenPinPolicy() && !TokenPinPolicy.validate(pin)) {
            throw new CodedException(X_TOKEN_PIN_POLICY_FAILURE, "Token PIN does not meet complexity requirements");
        }

        java.security.KeyPair kp = generateKeyPair(SystemProperties.getSignerKeyLength());

        String keyStoreFile = getKeyStoreFileName(PIN_FILE);
        savePkcs12Keystore(kp, PIN_ALIAS, keyStoreFile, pin);

        setTokenAvailable(tokenId, true);
        setTokenStatus(tokenId, TokenStatusInfo.OK);
    }

    private void rewriteKeyStoreWithNewPin(String keyFile, String keyAlias, char[] oldPin, char[] newPin) throws
            Exception {
        String keyStoreFile = getKeyStoreFileName(keyFile);
        String tempKeyStoreFile = getTempKeyStoreFileName(keyFile);

        // get key store by key store file name
        KeyStore oldKeyStore = loadPkcs12KeyStore(new File(keyStoreFile), oldPin);
        PrivateKey privateKey = SoftwareTokenUtil.loadPrivateKey(keyStoreFile, keyAlias, oldPin);

        KeyStore newKeyStore = KeyStore.getInstance("pkcs12");
        newKeyStore.load(null, null);
        Certificate[] certChain = oldKeyStore.getCertificateChain(keyAlias);
        KeyStore.PrivateKeyEntry pkEntry = new KeyStore.PrivateKeyEntry(privateKey, certChain);
        newKeyStore.setEntry(keyAlias, pkEntry, new KeyStore.PasswordProtection(newPin));

        try (FileOutputStream fos = new FileOutputStream(tempKeyStoreFile)) {
            newKeyStore.store(fos, newPin);
        }
    }

    private void handleUpdateTokenPin(char[] oldPin, char[] newPin) throws Exception {
        // TOBEDONE: Get the old pin from passwdstore instead of param
        // deactivateToken();
        // Create a temp folder
        createTempKeyDir();
        // Update the ".softtoken" keystore pin'
        rewriteKeyStoreWithNewPin(PIN_FILE, PIN_ALIAS, oldPin, newPin);
        // TOBEDONE: prevent token activation until pin change is done
        // Rewrite all keystores with the new PIN
        for (String keyId : listKeysOnDisk()) {
            rewriteKeyStoreWithNewPin(keyId, keyId, oldPin, newPin);
        }
    }

    private void activateToken() throws Exception {
        try {
            verifyPin(PasswordStore.getPassword(tokenId));

            setTokenStatus(tokenId, TokenStatusInfo.OK);
            setTokenActive(tokenId, true);
        } catch (FileNotFoundException e) {
            log.error("Software token not initialized", e);

            setTokenStatus(tokenId, TokenStatusInfo.NOT_INITIALIZED);

            throw tokenNotInitialized(tokenId);
        } catch (Exception e) {
            log.error("Error verifying token PIN", e);

            setTokenStatus(tokenId, TokenStatusInfo.USER_PIN_INCORRECT);

            throw CodedException.tr(X_PIN_INCORRECT, "pin_incorrect", "PIN incorrect");
        }
    }

    private void deactivateToken() {
        privateKeys.clear();

        setTokenActive(tokenId, false);
    }

    private PrivateKey loadPrivateKey(String keyId) throws Exception {
        String keyStoreFile = getKeyStoreFileName(keyId);

        log.trace("Loading pkcs#12 private key '{}' from file '{}'", keyId, keyStoreFile);

        return SoftwareTokenUtil.loadPrivateKey(keyStoreFile, keyId, getPin());
    }

    private String loadPublicKeyBase64(String keyId) throws Exception {
        String keyStoreFile = getKeyStoreFileName(keyId);

        log.trace("Loading pkcs#12 public key '{}' from file '{}'", keyId, keyStoreFile);

        java.security.cert.Certificate cert = loadCertificate(keyStoreFile, keyId, getPin());

        if (cert == null) {
            log.error("No certificate found in '{}' using alias '{}'", keyStoreFile, keyId);

            return null;
        }

        if (cert.getPublicKey() != null) {
            return encodeBase64(cert.getPublicKey().getEncoded());
        }

        return null;
    }

    private static void verifyPin(char[] pin) throws Exception {
        verifyPinProvided(pin);

        // Attempt to load private key from pin key store.
        SoftwareTokenUtil.loadPrivateKey(getKeyStoreFileName(PIN_FILE), PIN_ALIAS, pin);
    }

    private char[] getPin() throws Exception {
        final char[] pin = PasswordStore.getPassword(tokenId);
        verifyPinProvided(pin);

        return pin;
    }

    private static void verifyPinProvided(char[] pin) {
        if (pin == null || pin.length == 0) {
            throw new CodedException(X_INTERNAL_ERROR, "PIN not provided");
        }
    }

    private static void savePkcs12Keystore(KeyPair kp, String alias, String keyStoreFile, char[] password)
            throws Exception {
        KeyStore keyStore = createKeyStore(kp, alias, password);

        log.debug("Creating pkcs#12 keystore '{}'", keyStoreFile);

        try (FileOutputStream fos = new FileOutputStream(keyStoreFile)) {
            keyStore.store(fos, password);
        }
    }
}
