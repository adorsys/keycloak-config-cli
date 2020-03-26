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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ChecksumServiceTest {

    private ChecksumService checksumService;

    @BeforeEach
    public void setup() {
        checksumService = new ChecksumService();
    }

    @Test
    public void shouldThrowOnNullString() {
        String nullString = null;

        assertThrows(IllegalArgumentException.class, () -> checksumService.checksum(nullString));
    }

    @Test
    public void shouldThrowOnNullBytes() {
        byte[] nullBytes = null;

        assertThrows(IllegalArgumentException.class, () -> checksumService.checksum(nullBytes));
    }

    @Test
    public void shouldReturnChecksumForEmptyString() {
        String checksum = checksumService.checksum("");
        assertEquals("a69f73cca23a9ac5c8b567dc185a756e97c982164fe25859e0d1dcc1475c80a615b2123af1f5f94c11e3e9402c3ac558f500199d95b6d3e301758586281dcd26", checksum);
    }

    @Test
    public void shouldReturnChecksumForABC() {
        String checksum = checksumService.checksum("ABC");
        assertEquals("077aa33882b1aaf06da41c7ed3b6a40d7128dee23505ca2689c47637111c4701645fabc5ee1b9dcd039231d2d086bff9819ce2da8647432a73966494dd1a77ad", checksum);
    }

    @Test
    public void shouldReturnChecksumForABCasBytes() {
        String checksum = checksumService.checksum(new byte[]{65, 66, 67});
        assertEquals("077aa33882b1aaf06da41c7ed3b6a40d7128dee23505ca2689c47637111c4701645fabc5ee1b9dcd039231d2d086bff9819ce2da8647432a73966494dd1a77ad", checksum);
    }

    @Test
    public void shouldReturnChecksumForJson() {
        String checksum = checksumService.checksum("{\"property\":\"value\"}");
        assertEquals("118dd3237b94e86dc939bf28cdfbb24265101e754178c29b80f46efcaedc84aa5c2c9711a5b6438389c87f9f0ba0a2f105ec272412b69bcbeeba8eb96cfb7771", checksum);
    }
}
