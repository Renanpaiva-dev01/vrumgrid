package com.miaugrid.core.levels

import com.miaugrid.core.model.Puzzle
import kotlinx.serialization.json.Json

/** Fase pronta para a interface: número, metadados e o puzzle já validado. */
data class LevelInfo(val index: Int, val definition: LevelDefinition) {
    val id: String get() = definition.id
    val number: Int get() = definition.number
    val size: Int get() = definition.size
}

/**
 * Catálogo de fases carregado de um pacote JSON. As fases são convertidas sob demanda e
 * mantidas em cache; o pacote é verificado pelos testes automatizados.
 */
class LevelRepository(val pack: LevelPackDefinition) {

    private val cache = HashMap<String, Puzzle>()

    val levels: List<LevelInfo> = pack.levels.sortedBy { it.number }.mapIndexed { i, def -> LevelInfo(i, def) }

    val contentVersion: Int get() = pack.contentVersion

    fun level(id: String): LevelInfo? = levels.firstOrNull { it.id == id }

    fun next(id: String): LevelInfo? = level(id)?.let { levels.getOrNull(it.index + 1) }

    @Synchronized
    fun puzzle(id: String): Puzzle? {
        cache[id]?.let { return it }
        val def = level(id)?.definition ?: return null
        return def.toPuzzle().also { cache[id] = it }
    }

    companion object {
        const val CAMPAIGN_RESOURCE = "levels/campaign.json"

        val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

        fun fromJson(text: String): LevelRepository =
            LevelRepository(json.decodeFromString(LevelPackDefinition.serializer(), text))

        /** Carrega o pacote embutido no artefato do núcleo (funciona offline). */
        fun loadCampaign(): LevelRepository {
            val stream = LevelRepository::class.java.classLoader.getResourceAsStream(CAMPAIGN_RESOURCE)
                ?: error("Pacote de fases não encontrado: $CAMPAIGN_RESOURCE")
            return stream.bufferedReader(Charsets.UTF_8).use { fromJson(it.readText()) }
        }
    }
}
