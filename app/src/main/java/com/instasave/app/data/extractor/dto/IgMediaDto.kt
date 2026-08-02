package com.instasave.app.data.extractor.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IgMediaContainerDto(
    @SerialName("items") val items: List<IgItemDto>? = null,
    @SerialName("graphql") val graphql: IgGraphqlDto? = null
)

@Serializable
data class IgGraphqlDto(
    @SerialName("shortcode_media") val shortcodeMedia: IgItemDto? = null
)

@Serializable
data class IgItemDto(
    @SerialName("id") val id: String? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("shortcode") val shortcode: String? = null,
    @SerialName("media_type") val mediaType: Int? = null,
    @SerialName("is_video") val isVideo: Boolean? = null,
    @SerialName("video_versions") val videoVersions: List<IgVideoVersionDto>? = null,
    @SerialName("image_versions2") val imageVersions: IgImageContainerDto? = null,
    @SerialName("carousel_media") val carouselMedia: List<IgItemDto>? = null,
    @SerialName("edge_sidecar_to_children") val sidecarChildren: IgSidecarContainerDto? = null,
    @SerialName("caption") val captionObj: IgCaptionDto? = null,
    @SerialName("edge_media_to_caption") val edgeCaptionObj: IgEdgeCaptionContainerDto? = null,
    @SerialName("user") val user: IgUserDto? = null,
    @SerialName("owner") val owner: IgUserDto? = null,
    @SerialName("taken_at_timestamp") val takenAtTimestamp: Long? = null
)

@Serializable
data class IgVideoVersionDto(
    @SerialName("type") val type: Int? = null,
    @SerialName("width") val width: Int? = null,
    @SerialName("height") val height: Int? = null,
    @SerialName("url") val url: String? = null
)

@Serializable
data class IgImageContainerDto(
    @SerialName("candidates") val candidates: List<IgImageCandidateDto>? = null
)

@Serializable
data class IgImageCandidateDto(
    @SerialName("width") val width: Int? = null,
    @SerialName("height") val height: Int? = null,
    @SerialName("url") val url: String? = null
)

@Serializable
data class IgSidecarContainerDto(
    @SerialName("edges") val edges: List<IgSidecarEdgeDto>? = null
)

@Serializable
data class IgSidecarEdgeDto(
    @SerialName("node") val node: IgItemDto? = null
)

@Serializable
data class IgEdgeCaptionContainerDto(
    @SerialName("edges") val edges: List<IgEdgeCaptionDto>? = null
)

@Serializable
data class IgEdgeCaptionDto(
    @SerialName("node") val node: IgCaptionTextDto? = null
)

@Serializable
data class IgCaptionTextDto(
    @SerialName("text") val text: String? = null
)

@Serializable
data class IgCaptionDto(
    @SerialName("text") val text: String? = null
)

@Serializable
data class IgUserDto(
    @SerialName("pk") val pk: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("profile_pic_url") val profilePicUrl: String? = null
)
