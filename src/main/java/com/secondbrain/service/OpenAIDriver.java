package com.secondbrain.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary // Makes this the default implementation of BrainDriver
public class OpenAIDriver implements BrainDriver {

    private final ChatLanguageModel chatModel;

    public OpenAIDriver(@Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey) {
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o")
                .build();
    }

    @Override
    public String think(String context, String question) {
        try {
            // Construct the prompt with context
            String prompt = "You are a helpful AI Assistant named 'Second Brain'.\n" +
                    "Context information is below:\n" +
                    "---------------------\n" +
                    context + "\n" +
                    "---------------------\n" +
                    "Instructions:\n" +
                    "1. Answer the question based on the context provided.\n" +
                    "2. **FORMATTING**: Use bullet points, numbered lists, and short paragraphs to make the answer easy to read. Do NOT write large walls of text.\n"
                    +
                    "3. **CITATION**: If you use information from the context, append [Source: filename] at the end of the relevant sentence.\n"
                    +
                    "4. If the question is a greeting (e.g., 'Halo', 'Hi'), answer naturally without citations.\n" +
                    "5. Always answer in **Indonesian** unless asked otherwise.\n" +
                    "\n" +
                    "Question: " + question;

            // HASIL AI
            return chatModel.generate(prompt);

        } catch (Exception e) {
            return "Error from OpenAI: " + e.getMessage() + ". (Check your API Key)";
        }
    }
}
