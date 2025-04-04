### Instractions to Build and deploy Ollama Container to Cloud Run with pre downloaded model:

1.  **Make sure you have Cloud Run GPU quota:**

    -   To view and request quota http://g.co/cloudrun/gpu-quota
    -   To learn more about GPU redundancy option: http://g.co/cloudrun/gpu-redundancy-help


2.  **Update Dockerfile with the desiered model:**

    -   Edit `Dockerfile` to indicate the desiered model
    -   See https://ollama.com/library for the full list of available models.

3.  **Deploy to Cloud Run:**

    - `gcloud beta run deploy --source . ollama-gemma4b  --gpu-type=nvidia-l4 --region=us-central1  --port=11434 --no-gpu-zonal-redundancy --allow-unauthenticated --project=[$PROJECT_ID]`

4.  **Learn more:**

   - Cloud Run GPUs: https://cloud.google.com/run/docs/configuring/services/gpu
   - Full tutorial for Ollama on Cloud Run: https://cloud.google.com/run/docs/tutorials/gpu-gemma-with-ollama
   - Ollama: https://github.com/ollama/ollama
