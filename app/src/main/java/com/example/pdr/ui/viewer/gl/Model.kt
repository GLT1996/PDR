package com.example.pdr.ui.viewer.gl

/**
 * 3D 模型数据类
 * 包含顶点、法线和面数据
 */
data class Model(
    val vertices: FloatArray,      // 顶点坐标 [x, y, z, x, y, z, ...]
    val normals: FloatArray?,      // 法线向量 [nx, ny, nz, ...]
    val faces: IntArray?           // 面索引 [v1, v2, v3, ...] (每个面3个顶点)
) {
    /**
     * 获取顶点数量
     */
    fun getVertexCount(): Int = vertices.size / 3

    /**
     * 获取面数量
     */
    fun getFaceCount(): Int = faces?.size?.div(3) ?: (vertices.size / 9)

    /**
     * 是否有法线数据
     */
    fun hasNormals(): Boolean = normals != null && normals.isNotEmpty()

    /**
     * 计算模型边界盒，用于自动居中和缩放
     */
    fun getBoundingBox(): BoundingBox {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var maxZ = Float.MIN_VALUE

        for (i in vertices.indices step 3) {
            val x = vertices[i]
            val y = vertices[i + 1]
            val z = vertices[i + 2]

            minX = minOf(minX, x)
            minY = minOf(minY, y)
            minZ = minOf(minZ, z)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
            maxZ = maxOf(maxZ, z)
        }

        return BoundingBox(
            minX = minX, minY = minY, minZ = minZ,
            maxX = maxX, maxY = maxY, maxZ = maxZ
        )
    }

    /**
     * 模型边界盒
     */
    data class BoundingBox(
        val minX: Float, val minY: Float, val minZ: Float,
        val maxX: Float, val maxY: Float, val maxZ: Float
    ) {
        val centerX: Float get() = (minX + maxX) / 2
        val centerY: Float get() = (minY + maxY) / 2
        val centerZ: Float get() = (minZ + maxZ) / 2
        val sizeX: Float get() = maxX - minX
        val sizeY: Float get() = maxY - minY
        val sizeZ: Float get() = maxZ - minZ
        val maxDimension: Float get() = maxOf(sizeX, sizeY, sizeZ)
    }
}