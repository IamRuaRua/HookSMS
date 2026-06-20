package top.cuteruarua.hooksms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsSegmentMergerTest {

    @Test
    fun `merge combines multipart sms into one message`() {
        val merged = SmsSegmentMerger.merge(
            listOf(
                SmsSegment(
                    originatingAddress = "10086910",
                    messageBody = "【验证密码】303592，尊敬的用户，您的号码正在一个新设备上登录中国移动 APP，该验证码5分钟内有效。请勿泄露或转发他人，谨防被骗",
                    timestampMillis = 1_718_886_424_000
                ),
                SmsSegment(
                    originatingAddress = "10086910",
                    messageBody = "。【中国移动】",
                    timestampMillis = 1_718_886_424_000
                )
            )
        )

        requireNotNull(merged)
        assertEquals("10086910", merged.originatingAddress)
        assertEquals(
            "【验证密码】303592，尊敬的用户，您的号码正在一个新设备上登录中国移动 APP，该验证码5分钟内有效。请勿泄露或转发他人，谨防被骗。【中国移动】",
            merged.messageBody
        )
        assertEquals(1_718_886_424_000, merged.timestampMillis)
        assertEquals(2, merged.segmentCount)
    }

    @Test
    fun `merge returns null for empty parts`() {
        assertNull(SmsSegmentMerger.merge(emptyList()))
    }
}
