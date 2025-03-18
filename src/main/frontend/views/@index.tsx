import {nanoid} from "nanoid";
import {useEffect, useState} from "react";
import { Chat } from '@vaadin/flow-frontend/chat/Chat.js';
import {ChatEndpoint} from "Frontend/generated/endpoints";
import ChatOptions from "Frontend/generated/ai/patterns/web/endpoints/ChatEndpoint/ChatOptions";
import '@vaadin/icons';
import '@vaadin/vaadin-lumo-styles/icons';

import './index.css';
import {useForm} from "@vaadin/hilla-react-form";
import ChatOptionsModel from "Frontend/generated/ai/patterns/web/endpoints/ChatEndpoint/ChatOptionsModel";
import {Checkbox, ComboBox, TextArea, RadioGroup, RadioButton, Button, Icon, Tooltip} from "@vaadin/react-components";

const models = [
  'gemini-2.0-flash-001',
  'gemini-2.0-flash-thinking-exp-01-21',
  'gemini-2.0-flash-exp'
];

export enum ChunkingType {
  NONE = 'NONE',
  HIERARCHICAL = 'HIERARCHICAL',
  HYPOTHETICAL = 'HYPOTHETICAL',
  CONTEXTUAL = 'CONTEXTUAL',
  LATE = 'LATE'
}

export enum RetrievalType {
  NONE = 'NONE',
  FILTERING = 'FILTERING',
  QUERY_COMPRESSION = 'QUERY_COMPRESSION',
  QUERY_ROUTING = 'QUERY_ROUTING',
  HYDE = 'HYDE',
  RERANKING = 'RERANKING'
}

const defaultOptions: ChatOptions = {
  systemMessage: 'You are a knowledgeable history, geography and tourist assistant, knowing everything about the capitals of the world.\n\nYour role is to write reports about a particular location or event, focusing on the key topics asked by the user.\n\nLet us focus on world capitals today',
  useVertex: true,
  enableRAG: false,
  useAgents: false,
  useWebsearch: false,
  model: models[0],
  enableSafety: true,
  useGuardrails: false,
  evaluateResponse: false,
  chunkingType: ChunkingType.NONE,
  retrievalType: RetrievalType.NONE,
  writeActions: false,
  showDataSources: true
};

export default function AiPatterns() {
  const [chatId, setChatId] = useState(nanoid());

  const {field, model, read, value} = useForm(ChatOptionsModel);

  useEffect(() => {
    read(defaultOptions)
  }, []);

  async function resetChat() {
    setChatId(nanoid());
  }


  return (
    <div className="ai-patterns-ui">
      <header>
        <h1>World Capitals</h1>

        <Button onClick={resetChat} theme="icon small contrast tertiary">
          <Icon icon="lumo:reload" />
          <Tooltip slot="tooltip" text="New chat" />
        </Button>
      </header>
      <main>
        <div className="settings">
          <h3>LLMs</h3>
          <ComboBox label="Models" {...field(model.model)} items={models} />
          <TextArea label="System prompt" {...field(model.systemMessage)} />
          <h3>Chunking Methods</h3>
          <RadioGroup theme={"vertical"} {...field(model.chunkingType)}>
            <RadioButton value={ChunkingType.NONE} label="None (Prompt Stuffing)" />
            <RadioButton value={ChunkingType.HIERARCHICAL} label="Hierarchical Chunking" />
            <RadioButton value={ChunkingType.HYPOTHETICAL} label="Hypothetical Questions" />
            <RadioButton value={ChunkingType.CONTEXTUAL} label="Contextual Retrieval" />
            <RadioButton value={ChunkingType.LATE} label="Late Chunking" />
          </RadioGroup>
          <h3>Retrieval Methods</h3>
          <RadioGroup theme={"vertical"} {...field(model.retrievalType)}>
            <RadioButton value={ChunkingType.NONE} label="None" />
            <RadioButton value={RetrievalType.FILTERING} label="Filtering" />
            <RadioButton value={RetrievalType.QUERY_COMPRESSION} label="Query Compression" />
            <RadioButton value={RetrievalType.QUERY_ROUTING} label="Query Routing" />
            <RadioButton value={RetrievalType.HYDE} label="Hypothetical Document Embedding" />
            <RadioButton value={RetrievalType.RERANKING} label="Reranking" />
          </RadioGroup>
          <h3>Settings</h3>
          <Checkbox label="Use Vertex" {...field(model.useVertex)} />
          <h3>Capabilities</h3>
          <div className="vertical-checkboxes">
            <Checkbox label="Enable RAG" {...field(model.enableRAG)} />
            <Checkbox label="Use Agents" {...field(model.useAgents)} />
            <Checkbox label="Use WebSearch" {...field(model.useWebsearch)} />
          </div>
          <h3>Security</h3>
          <div className="side-by-side-checkboxes">
            <Checkbox label="Enable safety settings" {...field(model.enableSafety)} />
            <Checkbox label="Enable guardrails" {...field(model.useGuardrails)} />
          </div>
          <h3>Evaluations</h3>
          <Checkbox label="Evaluate responses" {...field(model.evaluateResponse)} />
          <h3>Advanced Capabilities</h3>
          <Checkbox label="Write actions" {...field(model.writeActions)} />
          <h3>Show sources</h3>
          <Checkbox label="Show data sources" {...field(model.showDataSources)} />
          <div className="space"></div>
          <div className="built-with">UI built in Java with <a href="https://vaadin.com/" target="_blank">Vaadin</a></div>
        </div>
        <Chat 
          chatId={chatId} 
          service={ChatEndpoint} 
          options={value} 
          acceptedFiles=".txt, .md"/>
      </main>
    </div>
  );
}