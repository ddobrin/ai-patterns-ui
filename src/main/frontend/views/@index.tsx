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
  systemMessage: '',
  useVertex: false,
  model: models[0],
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
          <h2>Settings</h2>
          <TextArea label="System prompt" {...field(model.systemMessage)} />
          <ComboBox label="Model" {...field(model.model)} items={models} />
          <Checkbox label="Use Vertex" {...field(model.useVertex)} />

          <div className="space"></div>
          <div className="built-with">UI built with <a href="https://vaadin.com/" target="_blank">Vaadin</a></div>
        </div>
        <Chat chatId={chatId} service={ChatEndpoint} options={value}/>
      </main>
    </div>
  );
}