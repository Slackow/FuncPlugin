package com.slackow.func.parser.value.types;

import com.slackow.func.parser.value.Type;
import com.slackow.func.parser.value.Value;

public class BooleanType extends Type {

    public static final BooleanType INSTANCE = new BooleanType();

    @Override
    public String getName() {
        return "boolean";
    }
}
