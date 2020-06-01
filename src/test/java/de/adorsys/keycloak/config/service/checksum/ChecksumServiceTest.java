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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
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
        assertThat(checksum, is("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
    }

    @Test
    public void shouldReturnChecksumForABC() {
        String checksum = checksumService.checksum("ABC");
        assertThat(checksum, is("b5d4045c3f466fa91fe2cc6abe79232a1a57cdf104f7a26e716e0a1e2789df78"));
    }

    @Test
    public void shouldReturnChecksumForABCasBytes() {
        String checksum = checksumService.checksum(new byte[]{65, 66, 67});
        assertThat(checksum, is("b5d4045c3f466fa91fe2cc6abe79232a1a57cdf104f7a26e716e0a1e2789df78"));
    }

    @Test
    public void shouldReturnChecksumForJson() {
        String checksum = checksumService.checksum("{\"property\":\"value\"}");
        assertThat(checksum, is("d7a04cbabf75c2d00df128c13c2b716a69597217351f54e3f3d8b715a28a9395"));
    }
}
