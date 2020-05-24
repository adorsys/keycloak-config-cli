/*
 * Copyright 2019-2020 adorsys GmbH & Co. KG @ https://adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package de.adorsys.keycloak.config.service.checksum;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class ChecksumService {

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public String checksum(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Cannot calculate checksum of null");
        }

        byte[] textInBytes = text.getBytes(StandardCharsets.UTF_8);
        return calculateChecksum(textInBytes);
    }

    public String checksum(byte[] textInBytes) {
        if (textInBytes == null) {
            throw new IllegalArgumentException("Cannot calculate checksum of null");
        }

        return calculateChecksum(textInBytes);
    }

    private String calculateChecksum(byte[] textInBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] shaInBytes = digest.digest(textInBytes);
            return bytesToHex(shaInBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
