package org.mediadownloader.data.remote.model

import com.google.gson.annotations.SerializedName

data class CobaltResponse(
    @SerializedName("status")  val status: String,
    @SerializedName("url")     val url: String?,
    @SerializedName("picker")  val picker: List<PickerItem>?,
    @SerializedName("error")   val error: CobaltError?
)

data class PickerItem(
    @SerializedName("url")  val url: String,
    @SerializedName("type") val type: String
)

data class CobaltError(
    @SerializedName("code") val code: String
)

data class VideoVariant(val url: String, val qualityLabel: String)
