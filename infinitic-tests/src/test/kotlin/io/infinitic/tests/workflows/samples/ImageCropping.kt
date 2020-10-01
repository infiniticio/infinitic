import io.infinitic.common.tasks.Task
import io.infinitic.common.workflows.*

interface ImageTask: Task {
    fun download(url: String): ByteArray
    fun crop(image: ByteArray, size: Int): ByteArray
    fun upload(image: ByteArray) : String
    fun save(email: String, urls: List<String>)
}

class ImageCropping() : Workflow {
    override lateinit var context: WorkflowTaskContext
    private val image = proxy(ImageTask::class.java)

    fun handle(email: String, imageUrl: String) {

        val img = image.download(imageUrl)

        val ds = mutableListOf<Deferred<String>>()
        for (size in 10..200) {
            ds.add(async {
                val img = image.crop(img, size)
                image.upload(img)
            })
        }

        val list = ds.and().result()

        image.save(email, list)
    }
}
