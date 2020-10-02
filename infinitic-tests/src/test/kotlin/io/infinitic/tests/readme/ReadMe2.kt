package io.infinitic.tests.readme//import io.infinitic.common.workflows.*
//
//interface ImageUtil {
//    fun download(url: String): ByteArray
//    fun crop(image: ByteArray, size: Int): ByteArray
//    fun upload(image: ByteArray) : String
//    fun save(email: String, urls: List<String>)
//}
//
//class ImageCropping() : Workflow {
//    override lateinit var context: WorkflowTaskContext
//    private val image = proxy(ImageUtil::class.java)
//
//    fun handle(email: String, imageUrl: String) {
//
//        val img = image.download(imageUrl)
//
//        val deferred60 = async {
//            val img60 = image.crop(img, 60)
//            image.upload(img60)
//        }
//        val deferred90 = async {
//            val img90 = image.crop(img, 90)
//            image.upload(img90)
//        }
//        val deferred120 = async {
//            val img120 = image.crop(img, 120)
//            image.upload(img120)
//        }
//
//        val list = (deferred60 and deferred90 and deferred120).result()
//
//        image.save(email, list)
//    }
//}
//
