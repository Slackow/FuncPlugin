package com.slackow.func.parser.value;

import java.util.HashMap;
import java.util.Map;

public class Value {
    private final Type type;
    private final Map<String, Value> properties = new HashMap<>();

    public Value(Type type) {
        this.type = type;
    }

    public String toJsonValue() {
        throw new UnsupportedOperationException();
    }

    public Type getType() {
        return type;
    }
}
