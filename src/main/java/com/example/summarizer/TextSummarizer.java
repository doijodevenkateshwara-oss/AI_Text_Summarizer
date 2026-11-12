package com.example.summarizer;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class TextSummarizer {

    private TextSummarizer() {
    }

    interface Summarizer {
        @SystemMessage("""
                You are an expert text summarization assistant. Your summaries are accurate,
                concise, and easy for a general audience to understand. Do not invent facts.
                """)
        @UserMessage("""
                Summarize the article below in {{detail}} detail.
                For the Summary section, use {{summaryInstructions}}.

                Article:
                {{text}}

                Return Markdown using exactly these three sections:

                ## Summary
                - bullet points only

                ## Keywords
                - important keyword

                ## Key Takeaways
                - bullet points only
                """)
        String summarize(@V("detail") String detail,
                         @V("summaryInstructions") String summaryInstructions,
                         @V("text") String text);
    }

    enum SummaryLength {
        SHORT("short", "3 concise bullet points"),
        MEDIUM("medium", "5-7 bullet points"),
        DETAILED("detailed", "8-12 bullet points");

        private final String label;
        private final String instructions;

        SummaryLength(String label, String instructions) {
            this.label = label;
            this.instructions = instructions;
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || args.length > 2 || isHelp(args[0])) {
            printUsage();
            return;
        }

        SummaryLength length;
        try {
            length = SummaryLength.valueOf(args.length == 2 ? args[1].toUpperCase(Locale.ROOT) : "MEDIUM");
        } catch (IllegalArgumentException e) {
            printUsage();
            return;
        }

        Map<String, String> dotenv = readDotEnv();
        String apiKey = setting(dotenv, "OPENAI_API_KEY", null);
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("OPENAI_API_KEY is required. Set it in the environment or in .env.");
            return;
        }

        OpenAiChatModel.OpenAiChatModelBuilder modelBuilder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(setting(dotenv, "OPENAI_MODEL", "gpt-4o-mini"));

        String baseUrl = setting(dotenv, "OPENAI_BASE_URL", null);
        if (baseUrl != null && !baseUrl.isBlank()) {
            modelBuilder.baseUrl(baseUrl);
        }

        ChatModel model = modelBuilder.build();
        Summarizer summarizer = AiServices.create(Summarizer.class, model);
        String text = readInput(args[0]);

        if (text.isBlank()) {
            System.err.println("Input text is empty.");
            return;
        }

        System.out.println(summarizer.summarize(length.label, length.instructions, text));
    }

    private static boolean isHelp(String arg) {
        return "--help".equals(arg) || "-h".equals(arg);
    }

    private static void printUsage() {
        System.out.println("""
                Usage: java -jar target/ai-text-summarizer.jar <text-file|-> [short|medium|detailed]

                Examples:
                  java -jar target/ai-text-summarizer.jar article.txt
                  java -jar target/ai-text-summarizer.jar article.txt short
                  cat article.txt | java -jar target/ai-text-summarizer.jar - detailed
                """);
    }

    private static String readInput(String source) throws IOException {
        if ("-".equals(source)) {
            return new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        }
        return Files.readString(Path.of(source), StandardCharsets.UTF_8);
    }

    private static Map<String, String> readDotEnv() {
        Path path = Path.of(".env");
        if (!Files.isRegularFile(path)) {
            return Map.of();
        }

        Map<String, String> values = new HashMap<>();
        try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
            lines.map(String::trim)
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .forEach(line -> {
                        if (line.startsWith("export ")) {
                            line = line.substring("export ".length()).trim();
                        }
                        int separator = line.indexOf('=');
                        if (separator <= 0) {
                            return;
                        }
                        String key = line.substring(0, separator).trim();
                        String value = line.substring(separator + 1).trim();
                        if (value.length() >= 2
                                && ((value.startsWith("\"") && value.endsWith("\""))
                                || (value.startsWith("'") && value.endsWith("'")))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        values.put(key, value);
                    });
        } catch (IOException e) {
            System.err.println("Warning: could not read .env: " + e.getMessage());
        }
        return values;
    }

    private static String setting(Map<String, String> dotenv, String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = dotenv.get(key);
        }
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
