package com.example.pdr.ui.viewer.parser

import com.example.pdr.ui.viewer.gl.Model
import java.io.BufferedReader
import java.io.InputStream

/**
 * OBJ 文件解析器
 * 支持: v (顶点), vn (法线), f (面)
 */
object ObjParser {

    /**
     * 解析 OBJ 文件输入流
     */
    fun parse(inputStream: InputStream): Model {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val tempVertices = mutableListOf<FloatArray>()
        val tempNormals = mutableListOf<FloatArray>()
        val faces = mutableListOf<Int>()

        BufferedReader(inputStream.reader()).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.trim().split("\\s+".toRegex())
                if (parts.isEmpty()) continue

                when (parts[0]) {
                    "v" -> {
                        // 顶点: v x y z
                        if (parts.size >= 4) {
                            tempVertices.add(floatArrayOf(
                                parts[1].toFloat(),
                                parts[2].toFloat(),
                                parts[3].toFloat()
                            ))
                        }
                    }
                    "vn" -> {
                        // 法线: vn x y z
                        if (parts.size >= 4) {
                            tempNormals.add(floatArrayOf(
                                parts[1].toFloat(),
                                parts[2].toFloat(),
                                parts[3].toFloat()
                            ))
                        }
                    }
                    "f" -> {
                        // 面: f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3
                        // 或: f v1 v2 v3
                        // 或: f v1//vn1 v2//vn2 v3//vn3
                        if (parts.size >= 4) {
                            // 支持三角形面
                            for (i in 1..3) {
                                val indices = parts[i].split("/")
                                val vertexIndex = indices[0].toInt() - 1 // OBJ 索引从 1 开始
                                faces.add(vertexIndex)

                                // 添加顶点坐标
                                if (vertexIndex in tempVertices.indices) {
                                    val v = tempVertices[vertexIndex]
                                    vertices.add(v[0])
                                    vertices.add(v[1])
                                    vertices.add(v[2])
                                }

                                // 添加法线 (如果有)
                                if (indices.size >= 3 && indices[2].isNotEmpty()) {
                                    val normalIndex = indices[2].toInt() - 1
                                    if (normalIndex in tempNormals.indices) {
                                        val n = tempNormals[normalIndex]
                                        normals.add(n[0])
                                        normals.add(n[1])
                                        normals.add(n[2])
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 如果没有法线，计算简单的面法线
        if (normals.isEmpty()) {
            calculateFaceNormals(vertices, normals)
        }

        return Model(
            vertices = vertices.toFloatArray(),
            normals = if (normals.isNotEmpty()) normals.toFloatArray() else null,
            faces = null // 顶点已经按面顺序排列
        )
    }

    /**
     * 计算面法线（简单实现）
     */
    private fun calculateFaceNormals(vertices: List<Float>, normals: MutableList<Float>) {
        for (i in vertices.indices step 9) {
            if (i + 8 < vertices.size) {
                val v0 = floatArrayOf(vertices[i], vertices[i + 1], vertices[i + 2])
                val v1 = floatArrayOf(vertices[i + 3], vertices[i + 4], vertices[i + 5])
                val v2 = floatArrayOf(vertices[i + 6], vertices[i + 7], vertices[i + 8])

                // 计算两条边
                val edge1 = floatArrayOf(v1[0] - v0[0], v1[1] - v0[1], v1[2] - v0[2])
                val edge2 = floatArrayOf(v2[0] - v0[0], v2[1] - v0[1], v2[2] - v0[2])

                // 叉积得到法线
                val normal = floatArrayOf(
                    edge1[1] * edge2[2] - edge1[2] * edge2[1],
                    edge1[2] * edge2[0] - edge1[0] * edge2[2],
                    edge1[0] * edge2[1] - edge1[1] * edge2[0]
                )

                // 归一化
                val length = kotlin.math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2])
                if (length > 0) {
                    normal[0] /= length
                    normal[1] /= length
                    normal[2] /= length
                }

                // 每个顶点使用相同的面法线
                for (j in 0..2) {
                    normals.add(normal[0])
                    normals.add(normal[1])
                    normals.add(normal[2])
                }
            }
        }
    }
}