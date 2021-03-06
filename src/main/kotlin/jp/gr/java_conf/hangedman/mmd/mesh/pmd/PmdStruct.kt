package jp.gr.java_conf.hangedman.mmd.mesh.pmd

import com.igormaznitsa.jbbp.mapper.Bin
import jp.gr.java_conf.hangedman.lwjgl.BufferBuilder.buildFloatBuffer
import jp.gr.java_conf.hangedman.lwjgl.BufferBuilder.buildShortBuffer
import jp.gr.java_conf.hangedman.mmd.mesh_if.Mesh
import jp.gr.java_conf.hangedman.mmd.mesh_if.Mesh.Companion.NUL
import java.io.File
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Vertex {
    @Bin var pos: FloatArray = floatArrayOf(0F, 0F, 0F)
    @Bin var normalVec: FloatArray = floatArrayOf(0F, 0F, 0F)
    @Bin var uv: FloatArray = floatArrayOf(0F, 0F)
    @Bin var boneNum: ShortArray = shortArrayOf(0, 0)
    @Bin var boneWeight: Byte = 0
    @Bin var edgeFlag: Byte = 0
}

class Material {
    @Bin var diffuseColor: FloatArray = floatArrayOf(0F, 0F, 0F)  // 物体色
    @Bin var alpha: Float = 0F                                    // 物体色透過率
    @Bin var specularity: Float = 0F                              // 光沢度
    @Bin var specularColor: FloatArray = floatArrayOf(0F, 0F, 0F) // 光沢色
    @Bin var ambientColor: FloatArray = floatArrayOf(0F, 0F, 0F)  // 環境色
    @Bin var toonIndex: Byte = 0                                  // toon番号
    @Bin var edgeFlag: Byte = 0                                   // エッジ
    @Bin var faceVertCount: Int = 0                               // 面頂点数
    @Bin var textureFileName: ByteArray = ByteArray(0)       // テクスチャーファイル名

    fun hasTexture(): Boolean {
        return !textureFileName.map { it.toChar() }.all { it == NUL }
    }

    fun texture(): String {
        return textureFileName
                .toString(charset = charset("Shift_JIS"))
                .let { s -> s.substring(0, s.indexOf(NUL)) }
    }
}

@Bin
class PmdStruct(override val meshPath: String) : Mesh {

    // ヘッダ
    @Bin(order = 1)
    var magic: ByteArray? = null

    @Bin(order = 2)
    var version: Float = 0F

    @Bin(order = 3)
    var modelName: ByteArray? = null

    @Bin(order = 4)
    var comment: ByteArray? = null

    // 頂点リスト
    @Bin(order = 5)
    var vertCount: Int = 0

    @Bin(order = 6, arraySizeExpr = "vertCount")
    var vertex: Array<Vertex>? = null

    // 面頂点リスト
    @Bin(order = 7)
    var faceVertCount: Int = 0

    @Bin(order = 8, arraySizeExpr = "faceVertCount")
    var faceVertIndex: ShortArray? = null

    // 材質リスト
    @Bin(order = 9)
    var materialCount: Int = 0

    @Bin(order = 10, arraySizeExpr = "materialCount")
    var material: Array<Material>? = null

    // 面頂点リストの適用合計値に対してどの材質リストを使うかを保持する連想配列
    private val materialRanged: List<Pair<Int, Material>> by lazy {
        this.material!!
                .mapIndexed { i, m ->
                    val materialRanged = this.material!!.filterIndexed { index, material ->
                        index <= i
                    }.map { it.faceVertCount }.sum()
                    materialRanged to m
                }
    }

    // 頂点に対してどの材質リストを使うかを保持する連想配列
    private val vertexMaterialMap: List<Pair<Int, Material>> by lazy {
        this.vertex!!
                .mapIndexed { index, _ ->
                    val faceVertIndex = this.faceVertIndex!!.indexOfFirst { faceVert -> faceVert == index.toShort() }
                    val material = materialRanged.find { m -> m.first >= faceVertIndex }
                    index to material!!.second
                }
    }

    override fun getModelInfo(): String {
        return this.comment!!
                .toString(charset = charset("Shift_JIS"))
                .run { this.substring(0, this.indexOf(NUL)) }
    }

    override fun verticesBuffer(): FloatBuffer {
        // 頂点リスト
        val vertices = this.vertex!!
                .map { v -> v.pos }
                .flatMap { fArray ->
                    mutableListOf<Float>().also {
                        it.addAll(fArray.asList())
                    }
                }.toFloatArray()
        return buildFloatBuffer(vertices)
    }

    override fun alphaBuffer(): FloatBuffer {

        val vertexMaterialMap = this.vertexMaterialMap

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val floatList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    floatList.add(m.alpha)
                    floatList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun diffuseColorsBuffer(): FloatBuffer {

        val vertexMaterialMap = this.vertexMaterialMap

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val floatList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    floatList.addAll(m.diffuseColor.toList())
                    floatList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun ambientColorsBuffer(): FloatBuffer {

        val vertexMaterialMap = this.vertexMaterialMap

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val floatList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    floatList.addAll(m.ambientColor.toList())
                    floatList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun specularColorsBuffer(): FloatBuffer {

        val vertexMaterialMap = this.vertexMaterialMap

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val floatList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    floatList.addAll(m.specularColor.toList())
                    floatList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun shininessBuffer(): FloatBuffer {

        val vertexMaterialMap = this.vertexMaterialMap

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val floatList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    floatList.add(m.specularity)
                    floatList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun edgeFlagBuffer(): FloatBuffer {

        val vertexMaterialMap = this.vertexMaterialMap

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val fList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    fList.add(m.edgeFlag.toFloat())
                    fList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun normalsBuffer(): FloatBuffer {
        val normals = this.vertex!!
                .map { v -> v.normalVec }
                .flatMap { fArray ->
                    mutableListOf<Float>().also {
                        it.addAll(fArray.asList())
                    }
                }.toFloatArray()

        return buildFloatBuffer(normals)
    }

    override fun faceVertPair(): Pair<Int, ShortBuffer> {
        return this.faceVertCount to buildShortBuffer(this.faceVertIndex!!)
    }

    override fun getModelYMax(): Float {
        return vertex!!.map{ v -> v.pos[1] }.max()!!
    }

    override fun getModelYMin(): Float {
        return vertex!!.map{ v -> v.pos[1] }.min()!!
    }

    override fun getTexturePaths(): List<String> {
        if (material.isNullOrEmpty())
            return emptyList()

        return material!!.filter { m ->
            // 空のファイルは除く
            m.hasTexture()
        }.map { m ->
            // Shift-JISは滅べ
            m.textureFileName.toString(charset = charset("Shift_JIS"))
        }.map { s ->
            // 終端文字以降はいらないので削り、モデルのあるパスを指定する
            "${File(meshPath).parent}${File.separator}${s.substring(0, s.indexOf(NUL))}"
        }
    }

    override fun texCoordBuffer(): FloatBuffer {

        return this.vertex!!
                .mapIndexed { _, v ->
                    val floatList = mutableListOf<Float>()
                    floatList.addAll(v.uv.toList())
                    floatList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun texLayerBuffer(): FloatBuffer {

        val vertexMaterialMap = this.vertexMaterialMap
        val texturePaths = getTexturePaths()

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val floatList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    if (m.hasTexture()) {
                        // テクスチャがあるのでその階層を返す
                        val index = texturePaths.indexOfFirst { path -> path.endsWith(m.texture()) }
                        floatList.add(index.toFloat())
                    } else {
                        // テクスチャがないのでとりあえず0fを返す
                        floatList.add(0f)
                    }
                    floatList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun sphereModeBuffer(): FloatBuffer {
        // -1f: スフィアなし、1f: スフィア乗算、2f: スフィア加算
        val vertexMaterialMap = this.vertexMaterialMap

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val floatList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    if (m.hasTexture()) {
                        // スフィアマップの種別を返す
                        when(File(m.texture()).extension) {
                            "sph" -> floatList.add(1f)
                            "spa" -> floatList.add(2f)
                            else  -> floatList.add(-1f)
                        }
                    } else {
                        // テクスチャがないので-1fを返す
                        floatList.add(-1f)
                    }
                    floatList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }
}
