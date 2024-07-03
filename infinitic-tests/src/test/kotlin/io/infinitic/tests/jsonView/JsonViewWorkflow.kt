/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.tests.jsonView

import com.fasterxml.jackson.annotation.JsonView
import io.infinitic.workflows.Workflow

internal interface JsonViewWorkflow {
  val request: Request?

  fun getUser_00(request: Request): User
  fun getUser_01(request: Request): User
  fun getUser_02(request: Request): User
  fun getUser_03(request: Request): User

  fun getUser_10(@JsonView(Views.Public::class) request: Request): User
  fun getUser_11(@JsonView(Views.Public::class) request: Request): User
  fun getUser_12(@JsonView(Views.Public::class) request: Request): User
  fun getUser_13(@JsonView(Views.Public::class) request: Request): User

  @JsonView(Views.Public::class)
  fun getUser_20(request: Request): User

  @JsonView(Views.Public::class)
  fun getUser_21(request: Request): User

  @JsonView(Views.Public::class)
  fun getUser_22(request: Request): User

  @JsonView(Views.Public::class)
  fun getUser_23(request: Request): User

  @JsonView(Views.Public::class)
  fun getUser_30(@JsonView(Views.Public::class) request: Request): User

  @JsonView(Views.Public::class)
  fun getUser_31(@JsonView(Views.Public::class) request: Request): User

  @JsonView(Views.Public::class)
  fun getUser_32(@JsonView(Views.Public::class) request: Request): User

  @JsonView(Views.Public::class)
  fun getUser_33(@JsonView(Views.Public::class) request: Request): User
}

@Suppress("unused")
internal class JsonViewWorkflowImpl : Workflow(), JsonViewWorkflow {

  private val jsonViewService = newService(JsonViewService::class.java)

  @JsonView(Views.Public::class)
  override var request: Request? = null

  override fun getUser_00(request: Request) = jsonViewService.getUser_0(request)
  override fun getUser_01(request: Request) = jsonViewService.getUser_1(request)
  override fun getUser_02(request: Request) = jsonViewService.getUser_2(request)
  override fun getUser_03(request: Request) = jsonViewService.getUser_3(request)

  override fun getUser_10(request: Request): User = jsonViewService.getUser_0(request)
  override fun getUser_11(request: Request): User = jsonViewService.getUser_1(request)
  override fun getUser_12(request: Request): User = jsonViewService.getUser_2(request)
  override fun getUser_13(request: Request): User = jsonViewService.getUser_3(request)

  override fun getUser_20(request: Request): User = jsonViewService.getUser_0(request)
  override fun getUser_21(request: Request): User = jsonViewService.getUser_1(request)
  override fun getUser_22(request: Request): User = jsonViewService.getUser_2(request)
  override fun getUser_23(request: Request): User = jsonViewService.getUser_3(request)

  override fun getUser_30(request: Request): User = jsonViewService.getUser_0(request)
  override fun getUser_31(request: Request): User = jsonViewService.getUser_1(request)
  override fun getUser_32(request: Request): User = jsonViewService.getUser_2(request)
  override fun getUser_33(request: Request): User = jsonViewService.getUser_3(request)
}


