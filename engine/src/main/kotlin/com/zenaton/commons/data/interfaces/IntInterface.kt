package com.zenaton.commons.data.interfaces

interface IntInterface {
    operator fun compareTo(i: Int): Int = int - i

    var int: Int
}
