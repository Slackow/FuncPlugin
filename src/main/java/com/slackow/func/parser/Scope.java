package com.slackow.func.parser;

import java.util.HashMap;

/**
 * A collection meant for variable storage, described the way scopes get deeper and then get shallower with every
 * left and right brace
 *
 * this is probably a bad description.
 * @param <T> type of variable
 */
public class Scope<T> extends HashMap<String, T> {
    public Scope<T> getParent() {
        return defaults;
    }

    private final Scope<T> defaults;

    private Scope(Scope<T> defaults) {
        this.defaults = defaults;
    }

    public Scope() {
        this(null);
    }

    public T getProperty(String key) {
        T value = super.get(key);
        return value == null && defaults != null ? defaults.getProperty(key) : value;
    }

    public void setProperty(String key, T value) {
        Scope<T> scope = this;
        while (scope != null && !scope.containsKey(key)) {
            scope = scope.getParent();
        }
        (scope == null ? this : scope).put(key,value);
    }

    public Scope<T> getNewChild() {
        return new Scope<>(this);
    }
}
