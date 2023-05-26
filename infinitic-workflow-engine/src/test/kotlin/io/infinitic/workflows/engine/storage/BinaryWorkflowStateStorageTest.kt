package io.infinitic.workflows.engine.storage

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.storage.keyValue.KeyValueStorage
import io.kotest.common.runBlocking
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.util.*
import kotlin.text.Charsets.UTF_8
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BinaryWorkflowStateStorageTest : CustomByteArrayCompression {
  private val storage = mockk<KeyValueStorage>()
  private val binaryWorkflowStateStorage = BinaryWorkflowStateStorage(storage, true)
  private val msg = TestFactory.random<WorkflowState>()
  private val workflowId = WorkflowId(UUID.randomUUID().toString())

  @BeforeEach
  fun init() {
    coEvery { storage.get(any()) } returns msg.toByteArray().gzipCompress()
    coEvery { storage.put(any(), any()) } just runs
  }

  @Test
  @DisplayName("should text be the same after compression and decompression")
  fun testCompression() {
    val lorem: String =
        """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Orci ac auctor augue mauris augue neque. Nunc sed augue lacus viverra. Et odio pellentesque diam volutpat commodo. Neque viverra justo nec ultrices dui sapien eget. Pulvinar proin gravida hendrerit lectus a. Volutpat diam ut venenatis tellus in metus. Nullam vehicula ipsum a arcu cursus. Amet nulla facilisi morbi tempus iaculis. Purus non enim praesent elementum. Pretium viverra suspendisse potenti nullam. Nullam ac tortor vitae purus faucibus. At urna condimentum mattis pellentesque id nibh. Convallis tellus id interdum velit laoreet id donec ultrices tincidunt. Turpis egestas sed tempus urna et pharetra.
      Purus viverra accumsan in nisl nisi scelerisque eu ultrices vitae. At consectetur lorem donec massa sapien faucibus et molestie ac. Neque viverra justo nec ultrices dui sapien eget mi proin. Sagittis id consectetur purus ut faucibus pulvinar elementum. Phasellus faucibus scelerisque eleifend donec pretium vulputate. Arcu cursus vitae congue mauris rhoncus aenean vel elit. Ornare aenean euismod elementum nisi quis eleifend quam adipiscing vitae. Donec pretium vulputate sapien nec. Suscipit tellus mauris a diam maecenas sed. Mollis aliquam ut porttitor leo.
      Nunc sed id semper risus. Felis donec et odio pellentesque diam volutpat. Mattis rhoncus urna neque viverra justo nec ultrices dui. Tempus urna et pharetra pharetra massa massa ultricies mi quis. Sit amet dictum sit amet justo donec enim diam. Tellus molestie nunc non blandit massa enim nec. Ac turpis egestas maecenas pharetra. Urna et pharetra pharetra massa massa. Cursus sit amet dictum sit amet justo donec. Leo duis ut diam quam nulla porttitor massa id neque. Proin fermentum leo vel orci porta non pulvinar neque laoreet. Et molestie ac feugiat sed lectus vestibulum mattis. Lacus sed viverra tellus in hac habitasse. Est velit egestas dui id ornare arcu odio. Vitae sapien pellentesque habitant morbi tristique senectus. Venenatis lectus magna fringilla urna porttitor rhoncus dolor purus non.
      Consequat ac felis donec et odio pellentesque. Justo donec enim diam vulputate ut. Semper feugiat nibh sed pulvinar. Consequat semper viverra nam libero justo laoreet. Ac odio tempor orci dapibus ultrices in iaculis nunc. Vitae congue mauris rhoncus aenean vel elit. Ut aliquam purus sit amet luctus venenatis lectus magna. Elementum nibh tellus molestie nunc non. Sit amet aliquam id diam maecenas ultricies mi. Lorem ipsum dolor sit amet. Massa ultricies mi quis hendrerit dolor. Tempor orci dapibus ultrices in iaculis nunc sed augue. Varius quam quisque id diam vel quam elementum pulvinar. Lacus vel facilisis volutpat est. Ante in nibh mauris cursus mattis molestie a. Id interdum velit laoreet id donec ultrices tincidunt arcu. Sem fringilla ut morbi tincidunt augue interdum.
      At tellus at urna condimentum mattis pellentesque id. Consequat semper viverra nam libero justo laoreet sit amet. Vestibulum lorem sed risus ultricies tristique nulla aliquet. Risus pretium quam vulputate dignissim suspendisse in est ante. Nibh nisl condimentum id venenatis a condimentum vitae sapien pellentesque. Nunc non blandit massa enim nec. Gravida dictum fusce ut placerat orci nulla pellentesque. Ut tortor pretium viverra suspendisse potenti nullam ac tortor vitae. Aliquam ut porttitor leo a diam sollicitudin. Nisl pretium fusce id velit ut tortor. Sed velit dignissim sodales ut. Amet purus gravida quis blandit. Hac habitasse platea dictumst vestibulum rhoncus est pellentesque elit. Diam donec adipiscing tristique risus nec feugiat in fermentum. Non curabitur gravida arcu ac tortor dignissim convallis.
       Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Orci ac auctor augue mauris augue neque. Nunc sed augue lacus viverra. Et odio pellentesque diam volutpat commodo. Neque viverra justo nec ultrices dui sapien eget. Pulvinar proin gravida hendrerit lectus a. Volutpat diam ut venenatis tellus in metus. Nullam vehicula ipsum a arcu cursus. Amet nulla facilisi morbi tempus iaculis. Purus non enim praesent elementum. Pretium viverra suspendisse potenti nullam. Nullam ac tortor vitae purus faucibus. At urna condimentum mattis pellentesque id nibh. Convallis tellus id interdum velit laoreet id donec ultrices tincidunt. Turpis egestas sed tempus urna et pharetra.
      Consequat ac felis donec et odio pellentesque. Justo donec enim diam vulputate ut. Semper feugiat nibh sed pulvinar. Consequat semper viverra nam libero justo laoreet. Ac odio tempor orci dapibus ultrices in iaculis nunc. Vitae congue mauris rhoncus aenean vel elit. Ut aliquam purus sit amet luctus venenatis lectus magna. Elementum nibh tellus molestie nunc non. Sit amet aliquam id diam maecenas ultricies mi. Lorem ipsum dolor sit amet. Massa ultricies mi quis hendrerit dolor. Tempor orci dapibus ultrices in iaculis nunc sed augue. Varius quam quisque id diam vel quam elementum pulvinar. Lacus vel facilisis volutpat est. Ante in nibh mauris cursus mattis molestie a. Id interdum velit laoreet id donec ultrices tincidunt arcu. Sem fringilla ut morbi tincidunt augue interdum.
      At tellus at urna condimentum mattis pellentesque id. Consequat semper viverra nam libero justo laoreet sit amet. Vestibulum lorem sed risus ultricies tristique nulla aliquet. Risus pretium quam vulputate dignissim suspendisse in est ante. Nibh nisl condimentum id venenatis a condimentum vitae sapien pellentesque. Nunc non blandit massa enim nec. Gravida dictum fusce ut placerat orci nulla pellentesque. Ut tortor pretium viverra suspendisse potenti nullam ac tortor vitae. Aliquam ut porttitor leo a diam sollicitudin. Nisl pretium fusce id velit ut tortor. Sed velit dignissim sodales ut. Amet purus gravida quis blandit. Hac habitasse platea dictumst vestibulum rhoncus est pellentesque elit. Diam donec adipiscing tristique risus nec feugiat in fermentum. Non curabitur gravida arcu ac tortor dignissim convallis.
    """
            .trimIndent()
    val stringToByteArray = lorem.toByteArray(UTF_8)
    val compressionByteArray = stringToByteArray.gzipCompress()
    assertTrue(compressionByteArray.size < stringToByteArray.size)
    val decompressionByteArray = compressionByteArray.gzipDecompress()
    assertEquals(String(decompressionByteArray), lorem)
  }

  @Test
  @DisplayName("should state be retrieve and equal after being store")
  fun getState() {
    runBlocking {
      val state = binaryWorkflowStateStorage.getState(workflowId)
      assertEquals(state, msg)
    }
  }

  @Test
  @DisplayName("should state be insert in db")
  fun putState() {
    runBlocking {
      binaryWorkflowStateStorage.putState(workflowId, msg)
      coVerify { storage.put(any(), any()) }
      confirmVerified(storage)
    }
  }
}
