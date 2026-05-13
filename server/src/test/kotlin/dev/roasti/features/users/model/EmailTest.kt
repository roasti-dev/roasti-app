package dev.roasti.features.users.model

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EmailTest {

  @Test
  fun `valid email is accepted`() {
    val result = Email.create("user@example.com")
    assertTrue(result.isRight())
  }

  @Test
  fun `missing at sign returns error`() {
    val result = Email.create("userexample.com")
    assertTrue(result.isLeft())
  }

  @Test
  fun `missing domain returns error`() {
    val result = Email.create("user@")
    assertTrue(result.isLeft())
  }

  @Test
  fun `missing local part returns error`() {
    val result = Email.create("@example.com")
    assertTrue(result.isLeft())
  }

  @Test
  fun `empty string returns error`() {
    val result = Email.create("")
    assertTrue(result.isLeft())
  }

  @Test
  fun `error contains message`() {
    val result = Email.create("notanemail")
    result.onLeft { assertNotNull(it.message) }
  }
}
