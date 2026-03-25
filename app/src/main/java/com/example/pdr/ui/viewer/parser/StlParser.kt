package com.example.pdr.ui.viewer.parser

import com.example.pdr.ui.viewer.gl.Model
import java.io.DataInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * STL 文件解析器
 * 支持二进制和 ASCII 格式
 */
object StlParser {

    /**
     * 解析 STL 文件输入流
     */
    fun parse(inputStream: InputStream): Model {
        return try {
            // 先尝试作为二进制格式解析
            parseBinary(inputStream)
        } catch (e: Exception) {
            // 如果失败，尝试 ASCII 格式
            inputStream.reset()
            parseAscii(inputStream)
        }
    }

    /**
     * 解析二进制 STL 文件
     * 格式:
     * - 80 字节头
     * - 4 字节三角形数量
     * - 每个三角形: 12 字节法线 + 36 字节顶点 + 2 字节属性
     */
    private fun parseBinary(inputStream: InputStream): Model {
        val dis = DataInputStream(inputStream)

        // 跳过 80 字节头
        dis.skipBytes(80)

        // 读取三角形数量
        val triangleCount = Integer.reverseBytes(dis.readInt())

        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()

        repeat(triangleCount) {
            // 读取法线
            val nx = Float.fromBits(Integer.reverseBytes(dis.readInt()))
            val ny = Float.fromBits(Integer.reverseBytes(dis.readInt()))
            val nz = Float.fromBits(Integer.reverseBytes(dis.readInt()))

            // 读取 3 个顶点
            repeat(3) {
                val x = Float.fromBits(Integer.reverseBytes(dis.readInt()))
                val y = Float.fromBits(Integer.reverseBytes(dis.readInt()))
                val z = Float.fromBits(Integer.reverseBytes(dis.readInt()))

                vertices.add(x)
                vertices.add(y)
                vertices.add(z)

                normals.add(nx)
                normals.add(ny)
                normals.add(nz)
            }

            // 跳过属性字节
            dis.skipBytes(2)
        }

        return Model(
            vertices = vertices.toFloatArray(),
            normals = normals.toFloatArray(),
            faces = null
        )
    }

    /**
     * 解析 ASCII STL 文件
     * 格式:
     * solid name
     *   facet normal ni nj nk
     *     outer loop
     *       vertex v1x v1y v1z
     *       vertex v2x v2y v2z
     *       vertex v3x v3y v3z
     *     endloop
     *   endfacet
     * endsolid
     */
    private fun parseAscii(inputStream: InputStream): Model {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()

        inputStream.bufferedReader().use { reader ->
            var line: String?
            var currentNormal = floatArrayOf(0f, 0f, 1f)

            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim().lowercase()

                when {
                    trimmed.startsWith("facet normal") -> {
                        val parts = trimmed.split("\\s+".toRegex())
                        if (parts.size >= 5) {
                            currentNormal = floatArrayOf(
                                parts[2].toFloat(),
                                parts[3].toFloat(),
                                parts[4].toFloat()
                            )
                        }
                    }
                    trimmed.startsWith("vertex") -> {
                        val parts = trimmed.split("\\s+".toRegex())
                        if (parts.size >= 4) {
                            vertices.add(parts[1].toFloat())
                            vertices.add(parts[2].toFloat())
                            vertices.add(parts[3].toFloat())

                            normals.add(currentNormal[0])
                            normals.add(currentNormal[1])
                            normals.add(currentNormal[2])
                        }
                    }
                }
            }
        }

        return Model(
            vertices = vertices.toFloatArray(),
            normals = normals.toFloatArray(),
            faces = null
        )
    }
}