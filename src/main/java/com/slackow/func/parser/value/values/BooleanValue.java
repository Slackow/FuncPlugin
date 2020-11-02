package com.slackow.func.parser.value.values;

import com.slackow.func.parser.value.Value;
import com.slackow.func.parser.value.types.BooleanType;

public class BooleanValue extends Value {

    private boolean value;

    public BooleanValue(Boolean b) {
        super(BooleanType.INSTANCE);
    }

    @Override
    public String toJsonValue() {
        return toString();
    }
}
