// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.intellij.build.images

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.SVGLoader
import java.awt.Dimension
import java.awt.Image
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.imageio.ImageIO

internal val File.children: List<File> get() = if (isDirectory) listFiles().toList() else emptyList()

internal fun isImage(file: Path, iconsOnly: Boolean): Boolean {
  if (!isImage(file)) return false
  return !iconsOnly || isIcon(file)
}

internal fun isIcon(file: Path): Boolean {
  if (!isImage(file)) return false
  val size = imageSize(file) ?: return false
  return size.height == size.width || size.height <= 100 && size.width <= 100
}

internal fun isImage(file: Path) = ImageExtension.fromName(file.fileName.toString()) != null

internal fun imageSize(file: Path): Dimension? {
  val image = loadImage(file)
  if (image == null) {
    println("WARNING: can't load $file")
    return null
  }
  val width = image.getWidth(null)
  val height = image.getHeight(null)
  return Dimension(width, height)
}

internal fun loadImage(file: Path): Image? {
  try {
    Files.newInputStream(file).buffered().use {
      if (file.fileName.toString().endsWith(".svg")) {
        return SVGLoader.load(it, 1.0f)
      }
      else {
        return ImageIO.read(it)
      }
    }
  }
  catch (e: Exception) {
    e.printStackTrace()
    return null
  }
}

internal fun md5(file: Path): String {
  val md5 = MessageDigest.getInstance("MD5")
  val hash = md5.digest(Files.readAllBytes(file))
  return BigInteger(hash).abs().toString(16)
}

internal enum class ImageType(private val suffix: String) {
  BASIC(""), RETINA("@2x"), DARCULA("_dark"), RETINA_DARCULA("@2x_dark");

  companion object {
    fun getBasicName(file: Path, prefix: List<String>): String {
      return getBasicName(file.fileName.toString(), prefix)
    }

    fun getBasicName(suffix: String, prefix: List<String>): String {
      val name = FileUtil.getNameWithoutExtension(suffix)
      return stripSuffix((prefix + name).joinToString("/"))
    }

    fun fromFile(file: Path): ImageType {
      return fromName(FileUtil.getNameWithoutExtension(file.fileName.toString()))
    }

    private fun fromName(name: String): ImageType {
      if (name.endsWith(RETINA_DARCULA.suffix)) return RETINA_DARCULA
      if (name.endsWith(RETINA.suffix)) return RETINA
      if (name.endsWith(DARCULA.suffix)) return DARCULA
      return BASIC
    }

    fun stripSuffix(name: String): String {
      val type = fromName(name)
      return name.removeSuffix(type.suffix)
    }
  }
}

internal enum class ImageExtension(private val suffix: String) {
  PNG(".png"), SVG(".svg"), GIF(".gif");

  companion object {
    fun fromFile(file: Path) = fromName(file.fileName.toString())

    fun fromName(name: String): ImageExtension? {
      if (name.endsWith(PNG.suffix)) return PNG
      if (name.endsWith(SVG.suffix)) return SVG
      if (name.endsWith(GIF.suffix)) return GIF
      return null
    }
  }
}