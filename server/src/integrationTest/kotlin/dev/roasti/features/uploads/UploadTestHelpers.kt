package dev.roasti.features.uploads

import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun generatePng(): ByteArray {
  val img = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
  val baos = ByteArrayOutputStream()
  ImageIO.write(img, "png", baos)
  return baos.toByteArray()
}

fun multipart(bytes: ByteArray, contentType: String, filename: String = "image.png") =
    MultiPartFormDataContent(
        formData {
          append(
              "file",
              bytes,
              Headers.build {
                append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                append(HttpHeaders.ContentType, contentType)
              },
          )
        }
    )
