/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AnyOfValidator extends BaseJsonValidator implements JsonValidator {
    private static final Logger logger = LoggerFactory.getLogger(RequiredValidator.class);

    private List<JsonSchema> schemas = new ArrayList<JsonSchema>();

    public AnyOfValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
        super(schemaPath, schemaNode, parentSchema, ValidatorTypeCode.ANY_OF, validationContext);
        int size = schemaNode.size();
        for (int i = 0; i < size; i++) {
            schemas.add(new JsonSchema(validationContext, getValidatorType().getValue(), schemaNode.get(i), parentSchema));
        }
    }

    public Set<ValidationMessage> validate(JsonNode node, JsonNode rootNode, String at) {
        debug(logger, node, rootNode, at);

        String typeValidatorName = "anyOf/type";
        JsonType nodeType = TypeFactory.getValueNodeType(node);
        //If schema has type validator and it doesn't match with node type then ignore it
        List<JsonSchema> filteredSchemaList = new ArrayList<JsonSchema>();
        List<String> expectedTypeList = new ArrayList<String>();
        for (JsonSchema schema : schemas) {
            if (schema.validators.containsKey(typeValidatorName)) {
                JsonType schemaType = ((TypeValidator) schema.validators.get(typeValidatorName)).getSchemaType();
                if (schemaType == nodeType) {
                    filteredSchemaList.add(schema);
                }
                expectedTypeList.add(schemaType.toString());
            }
            else {
                filteredSchemaList.add(schema);
            }
        }
        if (!schemas.isEmpty() && filteredSchemaList.isEmpty()) {
            return Collections.singleton(buildValidationMessage(at, StringUtils.join(expectedTypeList)));
        }

        Set<ValidationMessage> allErrors = new LinkedHashSet<ValidationMessage>();

        for (JsonSchema schema : filteredSchemaList) {
            Set<ValidationMessage> errors = schema.validate(node, rootNode, at);
            if (errors.isEmpty()) {
                return errors;
            }
            allErrors.addAll(errors);
        }

        return Collections.unmodifiableSet(allErrors);
    }

}
