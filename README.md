# ai-patterns-ui
AI Patterns w/UI front-end, running locally or deployed in Serverless GCP

## Anything in this repo is open to be changed, it is a shared working repo

#### Why this doc?
Developers are looking for guidance on implementing AI patterns in their enterprise applications.
Presenting such patterns in a UI-driven way is a good way to help them, especially when done in a workshop/conference/demo setting.

This doc is looking for guidance and examples on:
- how to present these patterns in a UI-driven way
- how to easily expand the sample app to include new patterns
- set up configuration for different models and credentials
- simplicity: idea on how to design a simple app, which is not all-consuming in terms of effort

#### Outcome:
- a sample app which will be extended in time, and updated with new patterns and models
- shared 
- retain the ability to be reused by all of us

#### Tech stack
- Spring Boot app 3.4.x
- Langchain4J 1.0.0-alpha* or GA
- Vaadin UI, w/Hilla
- Google AI + Google Vertex AI
- Locally / Cloud Run 
- Java 21 or 24 (post March 18)

#### where should it reside?
- for now in this repo
- a sample app is committed to this repo
- open-sourced when running

#### General ideas for using the app

(1) How should it look like, to be visiable, easily extendable?
A basic (strawman) mock-up of the UI:
![Basic mockup](image.png)

(2) Should patterns be grouped by Intro/simple, ingestion, consumption, evaluation, agents, to avoid overwhelming the user?
Conversely, how should the `Endpoint` classes be designed, assuming the same approach?

(3) Best approach to easily expand the sample app to include new patterns?
For the backend, either a new `Endpoint` class or a new method in an existing `Endpoint` class can easily be added, and it would always include the choice of credentials and the model to use

(4) How to set up configuration for different models and credentials?
Should be in the spring boot starters, with env values set for API Key; read at start-up, and then used in the `Endpoint` class, with the dropdowns to plainly/easily list the models avaialable. `Service` classes to be created to handle the interaction with the AI models.

(5) Should the System Message be displayed all the time, together wityh User Message and response, or only available in a button click?

(6) same question for thinking models and (whenever available in the API) deep research models?

#### Existing sample app:
[Vaadin and Spring Boot (Vertex Gemini, LLama 3.1 in Vertex, LLama 3.1 in GKE)](https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp/tree/main/sessions/fall24/spring-ai-quotes-llm-in-gke)

Model choice (drop-down?)
- Gemini 2.0 Flash Exp
- Gemini 2.0 Flash Thinking Exp
- Claude Sonnet 3.5 v2
- Llama 3.1 model-as-a-service
- Embedding models - text-embedding-005 from Google and Jina embedding v3 from Jina.ai

Credentials (drop-down?)
- API key
- Vertex AI (Google Cloud) credentials
- Jina.ai API Key
