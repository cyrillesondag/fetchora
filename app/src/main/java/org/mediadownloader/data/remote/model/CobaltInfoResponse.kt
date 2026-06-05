package org.mediadownloader.data.remote.model

import com.google.gson.annotations.SerializedName

data class CobaltInfoResponse(
    @SerializedName("cobalt")  val cobalt: CobaltInfo,
)

data class CobaltInfo(
    @SerializedName("version")  val version: String,
    @SerializedName("url") val url: String,
    @SerializedName("startTime") val startTime: Long,
    @SerializedName("services") val services: Array<String>,
    @SerializedName("git") val git: GitInfo,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CobaltInfo

        if (version != other.version) return false
        if (url != other.url) return false
        if (startTime != other.startTime) return false
        if (!services.contentEquals(other.services)) return false
        if (git != other.git) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + startTime.hashCode()
        result = 31 * result + services.contentHashCode()
        result = 31 * result + git.hashCode()
        return result
    }
}

data class GitInfo(
    @SerializedName("branch") val branch: String,
    @SerializedName("commit") val commit: String,
    @SerializedName("remote") val remote: String
)
