package com.example

import com.example.model.JunkCategoryType
import com.example.model.formatStorageSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testFormatStorageSize() {
    assertEquals("0 B", formatStorageSize(0))
    assertEquals("500 B", formatStorageSize(500))
    assertTrue(formatStorageSize(1024).contains("KB"))
    assertTrue(formatStorageSize(1024 * 1024).contains("MB"))
    assertTrue(formatStorageSize(1024L * 1024 * 1024 * 5).contains("GB"))
  }

  @Test
  fun testJunkCategoryDefaults() {
    assertTrue(JunkCategoryType.APP_CACHE.isSafeToDeleteByDefault)
    assertTrue(JunkCategoryType.TEMP_AND_LOGS.isSafeToDeleteByDefault)
    assertTrue(JunkCategoryType.EMPTY_FOLDERS.isSafeToDeleteByDefault)
  }
}
