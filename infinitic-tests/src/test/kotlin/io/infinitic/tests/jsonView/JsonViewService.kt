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

internal interface JsonViewService {
  fun getUser_0(request: Request): User

  fun getUser_1(@JsonView(Views.Public::class) request: Request): User

  @JsonView(Views.Public::class)
  fun getUser_2(request: Request): User

  @JsonView(Views.Public::class)
  fun getUser_3(@JsonView(Views.Public::class) request: Request): User
}

internal class JsonViewServiceImpl : JsonViewService {
  override fun getUser_0(request: Request): User = getUser(request)
  override fun getUser_1(request: Request): User = getUser(request)
  override fun getUser_2(request: Request): User = getUser(request)
  override fun getUser_3(request: Request): User = getUser(request)

  private fun getUser(request: Request) = User(request.userId).apply {
    company = request.company
    password = request.password
  }
}


internal class Request(
  @JsonView(Views.Public::class)
  val userId: String
) {
  @JsonView(Views.Internal::class)
  var company: String? = null

  var password: String? = null
}

internal class User(
  @JsonView(Views.Public::class)
  val userId: String
) {
  var company: String? = null

  @JsonView(Views.Internal::class)
  var password: String? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is User) return false

    if (userId != other.userId) return false
    if (company != other.company) return false
    if (password != other.password) return false

    return true
  }

  override fun hashCode(): Int {
    var result = userId.hashCode()
    result = 31 * result + (company?.hashCode() ?: 0)
    result = 31 * result + (password?.hashCode() ?: 0)
    return result
  }


  override fun toString(): String {
    return "User(userId='$userId', company=$company, password=$password)"
  }
}

internal class Views {
  open class Public
  class Internal : Public()
}
