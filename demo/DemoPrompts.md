#### Non-agentic SystemMessage
```
You are a knowledgeable history, geography and tourist assistant.  
Your role is to write reports about a particular location or event,  
focusing on the key topics asked by the user.  
  
Let us focus on world capitals today
```

User Message
```
Write a report about the population of Berlin
```

#### Agentic SystemMessage
```
You are a knowledgeable history, geography and tourist assistant.  
Your role is to write reports about a particular location or event,  
focusing on the key topics asked by the user.    
Think step by step:  
1) Identify the key topics the user is interested  
2) For each topic, devise a list of questions corresponding to those topics  
3) Search those questions in the database  
4) Collect all those answers together, and create the final report.  
```

#### Agentic SysteMessage with sources
```shell
You are a knowledgeable history, geography and tourist assistant.  
Your role is to write reports about a particular location or event,  
focusing on the key topics asked by the user.    
Think step by step:  
1) Identify the key topics the user is interested  
2) For each topic, devise a list of questions corresponding to those topics  
3) Search those questions in the database  
4) Collect all those answers together, and create the final report.  
5) For each answer to a topic, include the block following "#### Sources" to  your final report as-is
```

or
```shell
You are a knowledgeable history, geography and tourist assistant.  
Your role is to write reports about a particular location or event,  
focusing on the key topics asked by the user.    
Think step by step:  
1) Identify the key topics the user is interested  
2) For each topic, devise a list of questions corresponding to those topics  
3) Search those questions in the database  
4) Collect all those answers together, and create the final report.  
5) For each answer to a topic, include the block following "Please add at the end of your answer, the following content as-is, for reference purposes:

          #### Sources" to  your final report as-is
```
#### RAG UserMessages
Hypothetical Document Embedding
```
What's the capital of Germany?

How many people live there?
```

#### Planning UserMessage
```
Write a report about the population of Berlin, its geographic situation, its historical origins. Before proceeding to call any tool, return to me the list of steps you have identified and the list of questions you want to ask the tools available to you.
```

UserMessage
```
Write a report about the population of Berlin

Write a report about the population of Berlin, its geographic situation, and its historical origins
Write a report about the population of Berlin, its geographic situation, its historical origins and get the current temperature
Write a report about the cultural aspects of Berlin

# Currency user messages
What currency is used in the country of Germany?
What currency is used in the country of Germany and what is the exchange rate from USD to that currency?
Write a report about Berlin, including the currency in the country and the current exchange rate from the USD
Tell me what the currency is in Germany and the last update of the exchange rate from the USD

Write a report about the population of Berlin, its geographic situation, its historical origins, the currency in the country and the current exchange rate from the USD
Write a report about the population of Berlin, its geographic situation, its historical origins, the currency in the country and the last update of the exchange rate from the USD

# MCP User Messages

Write a report about the population of Berlin and find an article about the city in the archives

Write a report about the population of Berlin and list all the tools available in the TouristBureau of Berlin
List all the tools available in the TouristBureau of Berlin
List all the tools available in the TouristBureau in the city of Berlin

# List MCP tools
List all the tools available to you 
List all the tools available in the TouristBureau of Berlin
List all the tools available in the WeatherForecast for Berlin

Get the temperature for Berlin
Get the temperature for the capital with latitude 52.52437 and longitude 13.41053

# archived tests
Write a report about the population of Berlin, and get a printable article about the city
Write a report about the population of Berlin and find an article about the city in the archives
```

### Deployment

```
https://agentic-rag-360922367561.us-central1.run.app
```
Build and deploy
```
./mvnw spring-boot:build-image -DskipTests -Pproduction -Dspring-boot.build-image.imageName=agentic-rag

docker tag agentic-rag us-central1-docker.pkg.dev/genai-playground24/agentic-rag/agentic-rag:latest

docker push us-central1-docker.pkg.dev/genai-playground24/agentic-rag/agentic-rag:latest

gcloud run deploy agentic-rag --image us-central1-docker.pkg.dev/genai-playground24/agentic-rag/agentic-rag:latest  --region us-central1 --set-env-vars=SERVER_PORT=8080    --memory 2Gi --cpu 2 --cpu-boost --execution-environment=gen1 --set-env-vars=GCP_PROJECT_ID=genai-playground24 --set-env-vars=GCP_LOCATION=us-central1 
 --set-env-vars=ASTRA_TOKEN=AstraCS:DAyKLaFOtLfvNycaWqtGkLFt:2eb2d268182b2a68a3610fdbf6119a7384cee4635d23c4d06acfad788b3dc2e5
```
