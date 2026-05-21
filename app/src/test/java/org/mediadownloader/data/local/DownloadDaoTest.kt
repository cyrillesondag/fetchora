package org.mediadownloader.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.mediadownloader.data.local.db.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: DownloadDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = db.downloadDao()
    }

    @After
    fun tearDown() = db.close()

    private fun entity(id: String = "1") = DownloadEntity(
        id = id, tweetUrl = "https://x.com/user/status/$id",
        videoUrl = "https://video.twimg.com/video.mp4",
        fileName = "video_$id.mp4", filePath = "/Downloads/video_$id.mp4"
    )

    @Test
    fun `insert and retrieve by id`() = runTest {
        dao.insert(entity("abc"))
        val found = dao.findById("abc")
        assertNotNull(found)
        assertEquals("abc", found!!.id)
        assertEquals(DownloadStatus.DOWNLOADING, found.status)
    }

    @Test
    fun `update status to completed`() = runTest {
        dao.insert(entity("x"))
        dao.updateStatus("x", DownloadStatus.COMPLETED, 1_234_567L)
        val updated = dao.findById("x")!!
        assertEquals(DownloadStatus.COMPLETED, updated.status)
        assertEquals(1_234_567L, updated.fileSizeBytes)
    }

    @Test
    fun `flowAll emits inserted items`() = runTest {
        dao.insert(entity("1"))
        dao.insert(entity("2"))
        val list = dao.flowAll().first()
        assertEquals(2, list.size)
    }

    @Test
    fun `deleteById removes the row`() = runTest {
        dao.insert(entity("del"))
        dao.deleteById("del")
        assertNull(dao.findById("del"))
    }
}
