package com.slackow.func.parser

import java.util.*

/**
 * A collection meant for variable storage, described the way scopes get deeper and then get shallower with every
 * left and right brace
 *
 * this is probably a bad description.
 * @param <T> type of variable
</T> */
class Scope<T> private constructor(val parent: Scope<T>?) : HashMap<String, T>() {

    constructor() : this(null)

    fun getProperty(key: String): T? = super.get(key) ?: parent?.getProperty(key)

    fun setProperty(key: String, value: T) {
        var scope: Scope<T>? = this
        while (scope?.containsKey(key) == false) {
            scope = scope.parent
        }
        (scope ?: this)[key] = value
    }

    val newChild: Scope<T>
        get() = Scope(this)
}