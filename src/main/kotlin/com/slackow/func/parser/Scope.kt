package com.slackow.func.parser


/**
 * A collection meant for variable storage, described the way scopes get deeper and then get shallower with every
 * left and right brace
 *
 * this is probably a bad description.
 * @param <T> type of variable
</T> */
class Scope<T> private constructor(val parent: Scope<T>?) : HashMap<String, T>() {

    constructor() : this(null)

    override operator fun get(key: String): T? = getDirect(key) ?: parent?.get(key)

    fun getDirect(key: String) = super.get(key)

    fun setDirect(key: String, value: T) = super.put(key, value)

    operator fun set(key: String, value: T): T? {
        var scope: Scope<T>? = this
        while (scope != null && scope.getDirect(key) == null) {
            scope = scope.parent
        }
        (scope ?: this).setDirect(key, value)
        return value
    }

    val newChild: Scope<T>
        get() = Scope(this)
}