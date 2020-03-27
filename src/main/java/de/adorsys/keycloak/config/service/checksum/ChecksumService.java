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

import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;

@Service
public class ChecksumService {

    private final MessageDigest digest = new SHA3.Digest512();

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
