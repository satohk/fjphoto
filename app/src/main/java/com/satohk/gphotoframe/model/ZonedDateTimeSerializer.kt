package com.satohk.gphotoframe.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor: SerialDescriptor by lazy {
        PrimitiveSerialDescriptor(
            ZonedDateTimeSerializer::class.qualifiedName!!,
            PrimitiveKind.STRING
        )
    }

    private val formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssXXX")

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        return ZonedDateTime.parse(decoder.decodeString(), formatter)
    }
}