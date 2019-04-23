package de.adorsys.keycloak.config.service.checksum;

import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;

@Service
public class ChecksumService {

    private MessageDigest digest = new SHA3.Digest512();

    public String checksum(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Cannot calculate checksum of null");
        }

        byte[] textInBytes = text.getBytes();
        return calculateSha3Checksum(textInBytes);
    }

    public String checksum(byte[] textInBytes) {
        if (textInBytes == null) {
            throw new IllegalArgumentException("Cannot calculate checksum of null");
        }

        return calculateSha3Checksum(textInBytes);
    }

    private String calculateSha3Checksum(byte[] textInBytes) {
        byte[] shaInBytes = this.digest.digest(textInBytes);
        return Hex.toHexString(shaInBytes);
    }
}
