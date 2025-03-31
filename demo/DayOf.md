## First demo
```shell
What is the capital of France?
```
```shell
Who won most medals at the Paris Olympics in 2024?
```

## RAG
```shell
Write a report about the population of Berlin
```
Hypothetical Document Embedding
```
What's the capital of Germany?

How many people live there?
```




## Agents
UserMessage
```shell
You are a knowledgeable history, geography and tourist assistant.  
Your role is to write reports about a particular location or event,  
focusing on the key topics asked by the user.    
Think step by step:  
1) Identify the key topics the user is interested  
2) For each topic, devise a list of questions corresponding to those topics  
3) Search those questions in the database  
4) Collect all those answers together, and create the final report. 
```

```shell
Write a report about the population of Berlin, its geographic situation, and its historical origins
```

## Ollama calls
```shell
What currency is used in the country of Germany and what is the exchange rate from USD to that currency?
```

## MCP Calls
```shell
List all the tools available in the WeatherForecast for Berlin
```