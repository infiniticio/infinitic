import io.infinitic.common.workflows.*

interface ImageUtil {
    fun download(url: String): ByteArray
    fun crop(image: ByteArray, size: Int): ByteArray
    fun upload(image: ByteArray) : String
    fun save(email: String, urls: List<String>)
}

class ImageCropping() : Workflow {
    override lateinit var context: WorkflowTaskContext
    private val image = proxy(ImageUtil::class.java)

    fun handle(email: String, imageUrl: String) {

        val img = image.download(imageUrl)

        val ds = mutableListOf<Deferred<String>>()
        for (size in 10..200) {
            ds.add(async {
                val croppedImg = image.crop(img, size)
                image.upload(croppedImg)
            })
        }

        val list = ds.and().result()

        image.save(email, list)
    }
}


