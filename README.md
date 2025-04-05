# AI Patterns w/UI front-end, running locally or deployed in Serverless GCP

```shell
# start from debugger
AIPatternsWebApplication

# build from CLI and start in dev mode, changes in UI code automatically reflected
./mvnw spring-boot:run

# build runnable JAR
./mvnw clean package -Pproduction
 
 java -jar target/playground-0.0.1.jar
```
Env. variables: uses AstraDB, I will paste a key in the Git repo, under /Discussions - it is a private repo and the Astra account is free
```shell
export GCP_PROJECT_ID=<project>>
export GCP_PROJECT_NUM=<project_number>
export GCP_LOCATION=us-central1 
export ALLOY_DB_PASSWORD=...
export ALLOY_DB_URL=jdbc:postgresql://localhost:5432/library;
export ALLOY_DB_USERNAME=postgres
```

Deploy to Cloud Run
```shell
./mvnw spring-boot:build-image -DskipTests -Pproduction -Dspring-boot.build-image.imageName=agentic-rag

docker tag agentic-rag us-central1-docker.pkg.dev/genai-playground24/agentic-rag/agentic-rag:latest

docker push us-central1-docker.pkg.dev/genai-playground24/agentic-rag/agentic-rag:latest

gcloud run deploy agentic-rag --image us-central1-docker.pkg.dev/genai-playground24/agentic-rag/agentic-rag:latest  --region us-central1 --set-env-vars=SERVER_PORT=8080    --memory 2Gi --cpu 2 --cpu-boost --execution-environment=gen1 --set-env-vars=GCP_PROJECT_ID=genai-playground24 --set-env-vars=GCP_LOCATION=us-central1 
 --set-env-vars=ASTRA_TOKEN=<your secret>
 
# Note: replace the ASTRA_DB token with your own value, configure it as a secret
gcloud run deploy agentic-rag --image us-central1-docker.pkg.dev/genai-playground24/agentic-rag/agentic-rag:latest  --region us-central1 --set-env-vars=SERVER_PORT=8080    --memory 2Gi --cpu 2 --cpu-boost --execution-environment=gen1 --set-env-vars=GCP_PROJECT_ID=genai-playground24 --set-env-vars=GCP_LOCATION=us-central1 
 --set-env-vars=ASTRA_TOKEN=sm://genai-playground24/ASTRA_TOKEN/latest
```

