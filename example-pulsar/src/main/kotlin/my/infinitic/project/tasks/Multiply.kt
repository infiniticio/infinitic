package my.infinitic.project.tasks

interface Multiply {
    fun multiply(a: Int, b: Int): Int
}

class MultiplyImpl : Multiply {
    override fun multiply(a: Int, b: Int): Int = a * b
}
