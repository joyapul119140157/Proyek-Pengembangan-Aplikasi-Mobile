package com.studymate.data.repository

import com.studymate.data.remote.api.GeminiService
import com.studymate.data.remote.api.SystemPrompts
import com.studymate.domain.repository.AIRepository
import com.studymate.domain.repository.WritingStyle

class AIRepositoryImpl(
    private val geminiService: GeminiService
) : AIRepository {
    
    override suspend fun summarize(text: String): Result<String> {
        val prompt = """
            Rangkum teks berikut:
            
            $text
        """.trimIndent()
        
        return geminiService.generateContent(
            prompt = prompt,
            systemPrompt = SystemPrompts.SUMMARIZER
        )
    }
    
    override suspend fun generateIdeas(topic: String): Result<List<String>> {
        val prompt = """
            Berikan 5 ide kreatif untuk topik: $topic
        """.trimIndent()
        
        return geminiService.generateContent(
            prompt = prompt,
            systemPrompt = SystemPrompts.IDEA_GENERATOR
        ).map { response ->
            response.lines()
                .filter { it.isNotBlank() }
                .map { line ->
                    line.replace(Regex("^\\d+\\.\\s*"), "").trim()
                }
                .filter { it.isNotBlank() }
        }
    }
    
    override suspend fun improveWriting(text: String, style: WritingStyle): Result<String> {
        val styleInstruction = when (style) {
            WritingStyle.FORMAL -> "Gunakan gaya formal dan profesional."
            WritingStyle.CASUAL -> "Gunakan gaya santai dan friendly."
            WritingStyle.ACADEMIC -> "Gunakan gaya akademik dan ilmiah."
            WritingStyle.CREATIVE -> "Gunakan gaya kreatif dan menarik."
            WritingStyle.NEUTRAL -> "Gunakan gaya netral."
        }
        
        val prompt = """
            $styleInstruction
            
            Perbaiki tulisan berikut:
            
            $text
        """.trimIndent()
        
        return geminiService.generateContent(
            prompt = prompt,
            systemPrompt = SystemPrompts.WRITING_IMPROVER
        )
    }
    
    override suspend fun translate(text: String, targetLanguage: String): Result<String> {
        val prompt = """
            Terjemahkan ke bahasa $targetLanguage:
            
            $text
        """.trimIndent()
        
        return geminiService.generateContent(
            prompt = prompt,
            systemPrompt = SystemPrompts.TRANSLATOR
        )
    }
    
    override suspend fun chat(message: String): Result<String> {
        return geminiService.generateContent(prompt = message)
    }
    
    override suspend fun suggestTitle(content: String): Result<String> {
        val prompt = """
            Berikan saran judul untuk konten berikut:
            
            $content
        """.trimIndent()
        
        return geminiService.generateContent(
            prompt = prompt,
            systemPrompt = SystemPrompts.TITLE_SUGGESTER
        ).map { it.trim().removeSurrounding("\"") }
    }

    override suspend fun generateQuiz(content: String): Result<List<com.studymate.domain.model.Question>> {
        val prompt = """
            Buatlah 5 soal pilihan ganda berdasarkan teks berikut dalam format JSON.
            Setiap soal harus memiliki:
            - id: angka
            - question: teks pertanyaan
            - options: daftar 4 pilihan jawaban
            - correctAnswerIndex: indeks jawaban yang benar (0-3)
            - explanation: penjelasan singkat mengapa jawaban tersebut benar
            
            Teks:
            $content
        """.trimIndent()

        return geminiService.generateContent(
            prompt = prompt,
            systemPrompt = "You are an educational assistant that generates high-quality quizzes in JSON format."
        ).map { response ->
            // Simple JSON parsing logic - ideally use a proper parser but for now we extract the JSON part
            try {
                val jsonStartIndex = response.indexOf("[")
                val jsonEndIndex = response.lastIndexOf("]") + 1
                if (jsonStartIndex != -1 && jsonEndIndex != -1) {
                    val jsonString = response.substring(jsonStartIndex, jsonEndIndex)
                    kotlinx.serialization.json.Json.decodeFromString<List<com.studymate.domain.model.Question>>(jsonString)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
