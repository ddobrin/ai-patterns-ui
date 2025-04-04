From ollama/ollama

ENV HOME /root

WORKDIR /

ENV OLLAMA_FLASH_ATTENTION 1

# Pull models from Ollama. See https://ollama.com/library for the full list of available models.
RUN ollama serve & sleep 10 && ollama pull gemma3:4b

Entrypoint ["ollama", "serve"]
