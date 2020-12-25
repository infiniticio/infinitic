import io.infinitic.storage.kodein.Kodein
import io.infinitic.storage.kodein.KodeinStorage
import io.infinitic.storage.redis.toByteArray
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer

fun main() {
    val db = KodeinStorage(Kodein(path = "/Users/gilles/Sites/kodein/"))

    runBlocking {
        db.putState("test", ByteBuffer.wrap("first".toByteArray()))

        val b = db.getState("test")

        println(String(b!!.toByteArray()))
    }
}
