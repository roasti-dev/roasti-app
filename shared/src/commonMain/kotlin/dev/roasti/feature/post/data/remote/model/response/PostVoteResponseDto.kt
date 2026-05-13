package dev.roasti.feature.post.data.remote.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import dev.roasti.feature.post.data.remote.model.VoteDirectionDto

private object VoteDirectionDtoSerializer : KSerializer<VoteDirectionDto?> {
    override val descriptor = PrimitiveSerialDescriptor("VoteDirection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): VoteDirectionDto? {
        val value = decoder.decodeString().trim()
        return VoteDirectionDto.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }

    override fun serialize(encoder: Encoder, value: VoteDirectionDto?) {
        encoder.encodeString(value?.name?.lowercase() ?: "")
    }
}

@Serializable
data class PostVoteResponseDto(
    @SerialName("rating")
    val rating: Int,
    @Serializable(with = VoteDirectionDtoSerializer::class)
    @SerialName("user_vote")
    val userVote: VoteDirectionDto? = null,
)
