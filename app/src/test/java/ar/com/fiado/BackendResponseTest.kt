package ar.com.fiado

import ar.com.fiado.readings.readBackendResponse
import java.io.ByteArrayInputStream
import org.junit.Assert.*
import org.junit.Test

class BackendResponseTest {
 @Test fun readsCompleteResponseEvenWhenStreamReturnsSmallChunks() {
  val expected = ByteArray(20_000) { (it % 127).toByte() }
  val stream = object : ByteArrayInputStream(expected) {
   override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
    super.read(buffer, offset, minOf(length, 7))
  }
  assertArrayEquals(expected, readBackendResponse(stream))
 }
 @Test fun acceptsEmptyResponseAndExactSizeLimit() {
  assertArrayEquals(byteArrayOf(), readBackendResponse(ByteArrayInputStream(byteArrayOf())))
  val expected = ByteArray(2_000_000) { 42 }
  assertArrayEquals(expected, readBackendResponse(ByteArrayInputStream(expected)))
 }
 @Test fun readsOnlyOneExtraByteToDetectOversizedResponse() {
  val stream = ByteArrayInputStream(ByteArray(2_000_100))
  assertEquals(2_000_001, readBackendResponse(stream).size)
  assertEquals(99, stream.available())
 }
}