# AI Text Summarizer

A small Java 21 application that summarizes articles with LangChain4j and an OpenAI-compatible chat model.

The program accepts a text file (or standard input), then returns:

- A Markdown bullet-point summary
- Important keywords
- Key takeaways
- Short, medium, or detailed summary lengths

## Requirements

- Java 21 or newer
- Maven 3.8 or newer
- An OpenAI API key, or an API key for an OpenAI-compatible service

## Configuration

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

Edit `.env`:

```dotenv
OPENAI_API_KEY=sk-your-key-here
OPENAI_MODEL=gpt-4o-mini
# OPENAI_BASE_URL=https://api.openai.com/v1
```

Environment variables take priority over values in `.env`. `.env` is ignored by Git through `.gitignore`.

`OPENAI_MODEL` and `OPENAI_BASE_URL` are optional. `OPENAI_BASE_URL` can point to another OpenAI-compatible endpoint.

## Build

```bash
mvn -q -DskipTests package
```

This creates `target/ai-text-summarizer.jar`.

## Run

Default medium summary:

```bash
java -jar target/ai-text-summarizer.jar sample-article.txt
```

Short summary:

```bash
java -jar target/ai-text-summarizer.jar sample-article.txt short
```

Detailed summary:

```bash
java -jar target/ai-text-summarizer.jar sample-article.txt detailed
```

Read text from standard input:

```bash
cat sample-article.txt | java -jar target/ai-text-summarizer.jar - medium
```

Show usage:

```bash
java -jar target/ai-text-summarizer.jar --help
```

## Output format

The model is prompted to return Markdown similar to:

```markdown
## Summary
- ...
- ...

## Keywords
- ...
- ...

## Key Takeaways
- ...
- ...
```

## Troubleshooting

- `OPENAI_API_KEY is required`: create `.env` or export `OPENAI_API_KEY`.
- Very long articles may exceed the model context window. Use a model with a larger context window or shorten the input.
- Invalid summary length: use only `short`, `medium`, or `detailed`.
