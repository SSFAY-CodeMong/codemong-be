package com.codemong.be.global.kms;

import com.codemong.be.global.exception.CustomException;
import com.codemong.be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DisabledException;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.InvalidKeyUsageException;
import software.amazon.awssdk.services.kms.model.KmsException;
import software.amazon.awssdk.services.kms.model.NotFoundException;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class KmsService {

    private final KmsClient kmsClient;

    @Value("${aws.kms.key-id}")
    private String keyId;

    public String encrypt(String plainText) {
        try {
            SdkBytes plainBytes = SdkBytes.fromUtf8String(plainText);

            EncryptRequest encryptRequest = EncryptRequest.builder()
                    .keyId(keyId)
                    .plaintext(plainBytes)
                    .build();

            byte[] cipherBytes = kmsClient.encrypt(encryptRequest).ciphertextBlob().asByteArray();

            return Base64.getEncoder().encodeToString(cipherBytes);
        } catch (software.amazon.awssdk.services.kms.model.KmsException e) {
            throw mapKmsException(e, ErrorCode.KMS_ENCRYPTION_FAILED);
        }
    }

    public String decrypt(String cipherText) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(cipherText);

            SdkBytes cipherBytes = SdkBytes.fromByteArray(decodedBytes);

            DecryptRequest decryptRequest = DecryptRequest.builder()
                    .keyId(keyId)
                    .ciphertextBlob(cipherBytes)
                    .build();

            return kmsClient.decrypt(decryptRequest).plaintext().asUtf8String();
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.KMS_DECRYPTION_FAILED);
        } catch (KmsException e) {
            throw mapKmsException(e, ErrorCode.KMS_DECRYPTION_FAILED);
        }
    }

    private CustomException mapKmsException(KmsException e, ErrorCode defaultErrorCode) {
        if ("AccessDeniedException".equals(e.awsErrorDetails().errorCode())) {
            return new CustomException(ErrorCode.KMS_ACCESS_DENIED);
        }
        if (e instanceof NotFoundException || e instanceof DisabledException || e instanceof InvalidKeyUsageException) {
            return new CustomException(ErrorCode.KMS_KEY_UNAVAILABLE);
        }
        return new CustomException(defaultErrorCode);
    }
}
