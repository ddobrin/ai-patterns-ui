import 'highlight.js/styles/atom-one-light.css';
import { Icon } from '@vaadin/react-components';
import TypingIndicator from './TypingIndicator.js';
import CustomMarkdown from './CustomMarkdown.js';
import Message from 'Frontend/generated/ai/patterns/web/endpoints/AiChatService/Message.js';

interface MessageProps {
  message: Message;
  waiting?: boolean;
  renderer?: Parameters<typeof CustomMarkdown>[0]['renderer'];
}

export default function ChatMessage({ message, waiting, renderer }: MessageProps) {
  const hasAttachments = !!message.attachments?.length;

  return (
    <div
      className={`flex flex-col sm:flex-row gap-m p-m mt-m ${message.role !== 'assistant' ? 'me' : ''} ${waiting ? 'waiting-message' : ''}`}>
      <span className="text-2xl" hidden={message.role !== 'assistant'}>
        🤖
      </span>
      <div className="message-content">
        {waiting ? <TypingIndicator /> : null}

        {hasAttachments ? (
          <div className="attachments flex flex-col gap-s">
            {message.attachments?.map((attachment) => {
              if (attachment.type === 'image') {
                return <img key={attachment.key} src={attachment.url} alt={attachment.fileName} />;
              } else {
                return (
                  <div key={attachment.key} className="attachment flex gap-s">
                    <Icon icon="vaadin:file" />
                    <span>{attachment.fileName}</span>
                  </div>
                );
              }
            })}
          </div>
        ) : null}

        <CustomMarkdown content={message.content} renderer={renderer} />
      </div>
    </div>
  );
}
