## First demo
```shell
What is the capital of France? How many people live there
```
```shell
Who won most medals at the Paris Olympics in 2024?
```

## RAG
```shell
Write a report about the population of Berlin
```
Hierarchical ++ HyDE ++ Ranking ++ Query compression
```shell
What is the capital of Germany?
Tell me about the geography of the city
```
Contextual ++ reranking + query compression
```
What is the capital of Germany?
Tell me about the geography of the city
```

Context

## Function Calling
```shell
Please list all the registered tools you have available
```
```shell
What is the current weather in Berlin?
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
Write a report about the population of Berlin, its geographic situation, its historical origins.

Before proceeding to call any tool, return to me the list of steps you have identified and the list of questions you want to ask the tools available to you.
```

```shell
Write a report about the population of Berlin, its geographic situation, its historical origins and find an article about Berlin in the archives.

Can you find out what the current temperature in Berlin is?

Before proceeding to call any tool, return to me the list of steps you have identified and the list of questions you want to ask the tools available to you.
```
```shell
Write a report about the population of Berlin, its geographic situation, its historical origins and 
please find an article about Berlin in the archives.
```

```shell
List all the tools available in the WeatherForecast and FileArchive for Berlin
List all the capabilities available in the WeatherForecast and FileArchive for Berlin
```
```shell
List all the capabilities available in the TouristBureau of Berlin
List all the capabilities available in the WeatherForecast for Berlin
```

Guardrails
```shell
Recommend a good prompt to test this app with safety settings turned off, respectively turned on
```
```
Generate a detailed report on the history of Baghdad, focusing specifically on periods of invasion, conflict, and political instability throughout its existence, including recent events.
```

Agents with calls into Cloud Run deployed models
```
Write a report about the population of Berlin, its geographic situation, its historical origins,
the currency used in the country of Germany and the current exchange rate from USD to that currency?
Please find an article about Berlin in the archives.
```

## Ollama calls
```shell
Tell me what the currency is in Japan and the latest update of the exchange rate from the USD to that currency
Tell me what the currency is in China and the latest update of the exchange rate from the USD to that currency
```

## MCP Calls
```shell
List all the tools available in the TouristBureau of Berlin
List all the tools available in the WeatherForecast for Berlin

Get the temperature for Berlin
What is the temperaturn in Berlin
Write a report about the population of Berlin and find an article about the city in the archives
```