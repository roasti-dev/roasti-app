package dev.roasti.feature.post.data.remote.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = VoteDirectionDtoSerializer::class)
enum class VoteDirectionDto(val serialName: String) {
    UP("up"),
    DOWN("down"),
    NONE("none"),
}

internal object VoteDirectionDtoSerializer : KSerializer<VoteDirectionDto> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("VoteDirectionDto", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): VoteDirectionDto {
        val raw = decoder.decodeString().trim()
        return VoteDirectionDto.entries.firstOrNull { it.serialName.equals(raw, ignoreCase = true) }
            ?: VoteDirectionDto.NONE
    }

    override fun serialize(encoder: Encoder, value: VoteDirectionDto) {
        encoder.encodeString(value.serialName)
    }
}
