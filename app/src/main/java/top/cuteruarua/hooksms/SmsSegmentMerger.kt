package top.cuteruarua.hooksms

data class SmsSegment(
    val originatingAddress: String?,
    val messageBody: String?,
    val timestampMillis: Long
)

data class MergedSms(
    val originatingAddress: String?,
    val messageBody: String,
    val timestampMillis: Long,
    val segmentCount: Int
)

object SmsSegmentMerger {

    fun merge(parts: List<SmsSegment>): MergedSms? {
        if (parts.isEmpty()) {
            return null
        }

        val first = parts.first()
        return MergedSms(
            originatingAddress = first.originatingAddress,
            messageBody = parts.joinToString(separator = "") { it.messageBody.orEmpty() },
            timestampMillis = first.timestampMillis,
            segmentCount = parts.size
        )
    }
}
