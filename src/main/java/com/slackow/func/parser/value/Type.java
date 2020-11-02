package com.slackow.func.parser.value;

public abstract class Type extends Value {

    protected Type() {
        super(TypeType.INSTANCE);
    }

    public abstract String getName();

    @Override
    public String toString() {
        return "[" + getName() + "]";
    }

    private static class TypeType extends Type {
        private static final TypeType INSTANCE = new TypeType();

        @Override
        public String getName() {
            return "type";
        }
    }
}
