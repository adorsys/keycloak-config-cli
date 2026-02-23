/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.service.script;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.Map;

public class JavaScriptEvaluator implements ScriptEvaluator {

    @Override
    public Object evaluate(String expression, Map<String, Object> contextBindings) {
        try (Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(s -> false)
                .build()) {

            Value bindings = context.getBindings("js");
            for (Map.Entry<String, Object> entry : contextBindings.entrySet()) {
                bindings.putMember(entry.getKey(), entry.getValue());
            }

            Value result = context.eval("js", expression);
            return convertToValue(result);
        }
    }

    private Object convertToValue(Value value) {
        if (value.isNull()) {
            return null;
        } else if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            } else if (value.fitsInLong()) {
                return value.asLong();
            } else if (value.fitsInDouble()) {
                return value.asDouble();
            }
        } else if (value.isString()) {
            return value.asString();
        } else if (value.hasArrayElements()) {
            return value.as(Object.class);
        } else if (value.hasMembers()) {
            return value.as(Object.class);
        }
        return value.toString();
    }
}
