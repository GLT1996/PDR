package com.example.pdr.ui.viewer.gl

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * 网格渲染类
 * 负责将模型数据转换为 OpenGL 可渲染的缓冲区
 */
class Mesh(model: Model) {

    private var vertexBuffer: FloatBuffer
    private var normalBuffer: FloatBuffer?
    private var indexBuffer: ShortBuffer?
    private var indexCount: Int

    private var program = 0
    private var positionHandle = 0
    private var normalHandle = 0
    private var mvpMatrixHandle = 0
    private var mvMatrixHandle = 0
    private var lightPosHandle = 0
    private var colorHandle = 0

    init {
        Log.d("Mesh", "Creating mesh with ${model.getVertexCount()} vertices, ${model.getFaceCount()} faces")

        // 准备顶点缓冲区
        vertexBuffer = ByteBuffer.allocateDirect(model.vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(model.vertices)
        vertexBuffer.position(0)

        // 准备法线缓冲区
        normalBuffer = if (model.hasNormals()) {
            ByteBuffer.allocateDirect(model.normals!!.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(model.normals)
                    position(0)
                }
        } else {
            null
        }

        // 准备索引缓冲区
        indexBuffer = if (model.faces != null) {
            val indices = model.faces.map { it.toShort() }.toShortArray()
            indexCount = indices.size
            ByteBuffer.allocateDirect(indices.size * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .apply {
                    put(indices)
                    position(0)
                }
        } else {
            indexCount = model.getVertexCount()
            null
        }

        Log.d("Mesh", "Mesh created: indexCount=$indexCount, hasNormals=${normalBuffer != null}")
    }

    /**
     * 初始化 OpenGL 着色器程序
     */
    fun initShader() {
        // 顶点着色器
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uMVMatrix;
            uniform vec3 uLightPos;

            attribute vec4 aPosition;
            attribute vec3 aNormal;

            varying vec3 vNormal;
            varying vec3 vPosition;

            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vPosition = vec3(uMVMatrix * aPosition);
                vNormal = vec3(uMVMatrix * vec4(aNormal, 0.0));
            }
        """.trimIndent()

        // 片段着色器
        val fragmentShaderCode = """
            precision mediump float;

            uniform vec3 uLightPos;
            uniform vec4 uColor;

            varying vec3 vNormal;
            varying vec3 vPosition;

            void main() {
                vec3 lightDir = normalize(uLightPos - vPosition);
                vec3 normal = normalize(vNormal);

                // 环境光
                float ambient = 0.3;

                // 漫反射
                float diffuse = max(dot(normal, lightDir), 0.0);

                // 最终颜色
                gl_FragColor = uColor * (ambient + diffuse * 0.7);
            }
        """.trimIndent()

        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e("Mesh", "Failed to compile shaders")
            return
        }

        // 创建程序
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        // 检查链接状态
        val linked = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
        if (linked[0] == GLES20.GL_FALSE) {
            val log = GLES20.glGetProgramInfoLog(program)
            Log.e("Mesh", "Program link error: $log")
            GLES20.glDeleteProgram(program)
            program = 0
            return
        }

        // 获取属性和 uniform 位置
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        normalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        mvMatrixHandle = GLES20.glGetUniformLocation(program, "uMVMatrix")
        lightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")

        Log.d("Mesh", "Shader initialized: program=$program, positionHandle=$positionHandle, normalHandle=$normalHandle")
    }

    /**
     * 渲染网格
     */
    fun draw(mvpMatrix: FloatArray, mvMatrix: FloatArray, lightPos: FloatArray, color: FloatArray) {
        if (program == 0) {
            Log.e("Mesh", "Program not initialized")
            return
        }

        GLES20.glUseProgram(program)

        // 传递矩阵
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, mvMatrix, 0)
        GLES20.glUniform3fv(lightPosHandle, 1, lightPos, 0)
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        // 顶点属性
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        // 法线属性
        if (normalBuffer != null) {
            GLES20.glEnableVertexAttribArray(normalHandle)
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 12, normalBuffer)
        }

        // 绘制
        if (indexBuffer != null) {
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        } else {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, indexCount)
        }

        // 禁用属性
        GLES20.glDisableVertexAttribArray(positionHandle)
        if (normalBuffer != null) {
            GLES20.glDisableVertexAttribArray(normalHandle)
        }
    }

    /**
     * 加载着色器
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        // 检查编译状态
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == GLES20.GL_FALSE) {
            val log = GLES20.glGetShaderInfoLog(shader)
            Log.e("Mesh", "Shader compile error: $log")
            GLES20.glDeleteShader(shader)
            return 0
        }

        return shader
    }

    /**
     * 释放资源
     */
    fun release() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
    }
}