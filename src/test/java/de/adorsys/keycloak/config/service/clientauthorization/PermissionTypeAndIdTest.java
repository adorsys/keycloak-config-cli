/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2022 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.service.clientauthorization;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class PermissionTypeAndIdTest {

    public static Stream<Arguments> fromResourceName() {
        return Stream.of(
                Arguments.of("idp.resource.1dcbfbe7-1cee-4d42-8c39-d8ed74b4cf22",
                        new PermissionTypeAndId("idp", "1dcbfbe7-1cee-4d42-8c39-d8ed74b4cf22")),
                Arguments.of("idp.resource.$alias",
                        new PermissionTypeAndId("idp", "$alias")),
                Arguments.of("client.resource.$my-client-id",
                        new PermissionTypeAndId("client", "$my-client-id")),
                Arguments.of("role.resource.$My test role",
                        new PermissionTypeAndId("role", "$My test role")),
                Arguments.of("group.resource.$/My test group/My test group2",
                        new PermissionTypeAndId("group", "$/My test group/My test group2")),
                Arguments.of("something.resource.$id with $dollar",
                        new PermissionTypeAndId("something", "$id with $dollar")),
                Arguments.of("something.resource.$id.with.some.resource.in.it",
                        new PermissionTypeAndId("something", "$id.with.some.resource.in.it")),
                Arguments.of("bad format", null),
                Arguments.of("something.resource.", null),
                Arguments.of(".resource.xxx", null),
                Arguments.of("", null)
        );
    }

    public static Stream<Arguments> fromPolicyName() {
        return Stream.of(
                Arguments.of("token-exchange.permission.idp.1dcbfbe7-1cee-4d42-8c39-d8ed74b4cf22",
                        new PermissionTypeAndId("idp", "1dcbfbe7-1cee-4d42-8c39-d8ed74b4cf22")),
                Arguments.of("manage.permission.client.$my-client-id",
                        new PermissionTypeAndId("client", "$my-client-id")),
                Arguments.of("map-role.permission.$test role",
                        new PermissionTypeAndId("role", "$test role")),
                Arguments.of("manage.members.permission.group.$/My test group/My test group2",
                        new PermissionTypeAndId("group", "$/My test group/My test group2")),
                Arguments.of("some.scope.permission.something.$id with $dollar",
                        new PermissionTypeAndId("something", "$id with $dollar")),
                Arguments.of("some.scope.permission.something.$id.with.some.permission.in.it",
                        new PermissionTypeAndId("something", "$id.with.some.permission.in.it")),
                Arguments.of("map-role-xxxx.permission.$id with $dollar",
                        new PermissionTypeAndId("role", "$id with $dollar")),
                Arguments.of("map-role-xxxx.permission.$my.id",
                        new PermissionTypeAndId("role", "$my.id")),
                Arguments.of("map-role-xxxx.permission.$id.with.some.permission.in.it",
                        new PermissionTypeAndId("role", "$id.with.some.permission.in.it")),
                Arguments.of("map-roles.permission.client.bd536a1f-7666-4b7b-bc6a-5875b8ef1a91",
                        new PermissionTypeAndId("client", "bd536a1f-7666-4b7b-bc6a-5875b8ef1a91")),
                Arguments.of("bad format", null),
                Arguments.of("map-role-xxx.permission.", null),
                Arguments.of("map-role.permission.", null),
                Arguments.of("map-role", null),
                Arguments.of("token-exchange.permission.idp.", null),
                Arguments.of("token-exchange.permission.", null),
                Arguments.of(".permission.xx", null),
                Arguments.of(".permission.", null),
                Arguments.of("", null)
        );
    }

    @ParameterizedTest
    @MethodSource
    void fromResourceName(String resourceName, PermissionTypeAndId expected) {
        PermissionTypeAndId actual = PermissionTypeAndId.fromResourceName(resourceName);
        assertThat(actual, is(equalTo(expected)));
    }

    @ParameterizedTest
    @MethodSource
    void fromPolicyName(String policyName, PermissionTypeAndId expected) {
        PermissionTypeAndId actual = PermissionTypeAndId.fromPolicyName(policyName);
        assertThat(actual, is(equalTo(expected)));
    }
}
