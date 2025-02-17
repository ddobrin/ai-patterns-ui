import {nanoid} from "nanoid";
import {useEffect, useState} from "react";
import { Chat } from '@vaadin/flow-frontend/chat/Chat.js';
import {ChatEndpoint} from "Frontend/generated/endpoints";
import ChatOptions from "Frontend/generated/ai/patterns/web/endpoints/ChatEndpoint/ChatOptions";

import './index.css';
import {useForm} from "@vaadin/hilla-react-form";
import ChatOptionsModel from "Frontend/generated/ai/patterns/web/endpoints/ChatEndpoint/ChatOptionsModel";
import {Checkbox, ComboBox, TextArea} from "@vaadin/react-components";

const models = [
  'gemini-2.0-flash-001',
  'gemini-2.0-flash-thinking-exp-01-21',
  'gemini-2.0-pro-exp-02-05'
];

const defaultOptions: ChatOptions = {
  systemMessage: 'You are a knowledgeable history, geography and tourist assistant.\n\nYour role is to write reports about a particular location or event, focusing on the key topics asked by the user.\n\nLet us focus on world capitals today',
  useVertex: true,
  useTools: false,
  useAgents: false,
  model: models[0],
  useGuardrails: false,
  evaluateResponse: false
};

export default function AiPatterns() {
  const [chatId] = useState(nanoid());

  const {field, model, read, value} = useForm(ChatOptionsModel);

  useEffect(() => {
    read(defaultOptions)
  }, []);

  return (
    <div className="ai-patterns-ui">
      <header>
        <h1>World Capitals</h1>
      </header>
      <main>
        <div className="settings">
          <h3>LLMs</h3>
          <ComboBox label="Models" {...field(model.model)} items={models} />
          <TextArea label="System prompt" {...field(model.systemMessage)} />
          <h3>Settings</h3>
          <Checkbox label="Use Vertex" {...field(model.useVertex)} />
          <h3>Capabilities</h3>
          <Checkbox label="Use Tools" {...field(model.useTools)} />
          <Checkbox label="Use Agents" {...field(model.useAgents)} />
          <h3>Guardrails</h3>
          <Checkbox label="Enable guardrails" {...field(model.useGuardrails)} />
          <h3>Evaluations</h3>
          <Checkbox label="Evaluate response" {...field(model.evaluateResponse)} />
          <div className="space"></div>
          <div className="built-with">UI built with <a href="https://vaadin.com/" target="_blank">Vaadin</a></div>
        </div>
        <Chat chatId={chatId} service={ChatEndpoint} options={value}/>
      </main>
    </div>
  );
}