package my.infinitic.project.tasks

interface Add {
    fun add(a: Int, b: Int): Int
}

class AddImpl : Add {
    override fun add(a: Int, b: Int): Int = a + b
}
