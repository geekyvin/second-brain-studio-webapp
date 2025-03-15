
import { useState, useEffect, useRef } from "react";
import { Button } from "../components/ui/button";
import { Textarea } from "../components/ui/textarea";
import { Sparkles, Save } from "lucide-react";
import { useToast } from "../hooks/use-toast";
import ReactMarkdown from "react-markdown";

const NoteEditor = () => {
  const [content, setContent] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [focusedLineIndex, setFocusedLineIndex] = useState<number | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const { toast } = useToast();
  
  useEffect(() => {
    const timer = setTimeout(() => {
      setIsTyping(false);
    }, 1000);
    
    return () => clearTimeout(timer);
  }, [content]);
  
  const handleContentChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value);
    setIsTyping(true);
    
    const textArea = e.target;
    const selectionStart = textArea.selectionStart;
    const textBeforeCursor = textArea.value.substring(0, selectionStart);
    const currentLineIndex = textBeforeCursor.split('\n').length - 1;
    setFocusedLineIndex(currentLineIndex);
  };
  
  const handleFocus = () => {
    if (textareaRef.current) {
      const selectionStart = textareaRef.current.selectionStart;
      const textBeforeCursor = textareaRef.current.value.substring(0, selectionStart);
      const currentLineIndex = textBeforeCursor.split('\n').length - 1;
      setFocusedLineIndex(currentLineIndex);
    }
  };
  
  const handleBlur = () => {
    setFocusedLineIndex(null);
  };
  
  const handleClick = (e: React.MouseEvent<HTMLTextAreaElement>) => {
    handleFocus();
  };

  const focusLine = (lineIndex: number, position: 'start' | 'end' | number = 'start') => {
    setFocusedLineIndex(lineIndex);
    setTimeout(() => {
      if (textareaRef.current) {
        textareaRef.current.focus();
        
        if (position === 'start') {
          textareaRef.current.selectionStart = 0;
          textareaRef.current.selectionEnd = 0;
        } else if (position === 'end') {
          textareaRef.current.selectionStart = textareaRef.current.value.length;
          textareaRef.current.selectionEnd = textareaRef.current.value.length;
        } else {
          textareaRef.current.selectionStart = position;
          textareaRef.current.selectionEnd = position;
        }
      }
    }, 10);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>, lineIndex: number) => {
    const contentLines = content.split('\n');
    const currentLine = contentLines[lineIndex];
    const textArea = e.currentTarget;
    const cursorPosition = textArea.selectionStart;
    const isAtLineStart = cursorPosition === 0;
    const isAtLineEnd = cursorPosition === currentLine.length;
    
    if (e.key === 'Backspace' && isAtLineStart && lineIndex > 0) {
      e.preventDefault();
      
      const newLines = [...contentLines];
      const previousLine = newLines[lineIndex - 1];
      
      newLines.splice(lineIndex, 1);
      setContent(newLines.join('\n'));
      
      focusLine(lineIndex - 1, 'end');
    }
    
    else if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      
      const beforeCursor = textArea.selectionStart;
      
      const textBeforeCursor = currentLine.substring(0, beforeCursor);
      const textAfterCursor = currentLine.substring(beforeCursor);
      
      const newLines = [...contentLines];
      newLines[lineIndex] = textBeforeCursor;
      newLines.splice(lineIndex + 1, 0, textAfterCursor);
      
      setContent(newLines.join('\n'));
      
      focusLine(lineIndex + 1);
    }
    
    else if (e.key === 'ArrowUp' && !e.shiftKey) {
      if (lineIndex > 0) {
        e.preventDefault();
        const prevLine = contentLines[lineIndex - 1];
        const targetPosition = Math.min(cursorPosition, prevLine.length);
        focusLine(lineIndex - 1, targetPosition);
      }
    }
    
    else if (e.key === 'ArrowDown' && !e.shiftKey) {
      if (lineIndex < contentLines.length - 1) {
        e.preventDefault();
        const nextLine = contentLines[lineIndex + 1];
        const targetPosition = Math.min(cursorPosition, nextLine.length);
        focusLine(lineIndex + 1, targetPosition);
      }
    }
    
    else if (e.key === 'ArrowLeft' && isAtLineStart && lineIndex > 0) {
      e.preventDefault();
      focusLine(lineIndex - 1, 'end');
    }
    
    else if (e.key === 'ArrowRight' && isAtLineEnd && lineIndex < contentLines.length - 1) {
      e.preventDefault();
      focusLine(lineIndex + 1, 'start');
    }
  };
  
  const handleGenerateContent = () => {
    toast({
      title: "Generating content",
      description: "Your AI-generated content will appear shortly",
    });
  };
  
  const handleSave = () => {
    toast({
      title: "Note saved",
      description: "Your note has been saved successfully",
    });
  };
  
  const renderObsidianStyleEditor = () => {
    const contentLines = content.split('\n');
    
    return (
      <div className="flex-1 p-6 overflow-auto">
        {contentLines.length === 0 || content === '' ? (
          <Textarea
            ref={textareaRef}
            placeholder="Start writing your thoughts here..."
            className="min-h-[calc(100vh-200px)] resize-none border-0 focus-visible:ring-0 focus-visible:ring-offset-0 text-base"
            value={content}
            onChange={handleContentChange}
            onFocus={handleFocus}
            onBlur={handleBlur}
            onClick={handleClick}
            onKeyDown={(e) => handleKeyDown(e, 0)}
          />
        ) : (
          <div className="min-h-[calc(100vh-200px)]">
            {contentLines.map((line, index) => (
              <div key={index} className="relative">
                {index === focusedLineIndex ? (
                  <textarea
                    ref={index === focusedLineIndex ? textareaRef : null}
                    value={line}
                    onChange={(e) => {
                      const newLines = [...contentLines];
                      newLines[index] = e.target.value;
                      setContent(newLines.join('\n'));
                      setIsTyping(true);
                    }}
                    onKeyDown={(e) => handleKeyDown(e, index)}
                    className="w-full resize-none border-0 focus-visible:ring-0 focus-visible:ring-offset-0 text-base p-1 outline-none"
                    onBlur={handleBlur}
                    autoFocus
                    rows={1}
                    style={{ minHeight: '24px', height: 'auto' }}
                  />
                ) : (
                  <div 
                    className="prose prose-gray max-w-none p-1 cursor-text"
                    onClick={() => {
                      setFocusedLineIndex(index);
                      setTimeout(() => {
                        if (textareaRef.current) {
                          textareaRef.current.focus();
                        }
                      }, 10);
                    }}
                  >
                    <ReactMarkdown>{line || " "}</ReactMarkdown>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    );
  };
  
  return (
    <div className="h-full flex flex-col animate-fade-in">
      {renderObsidianStyleEditor()}
      <div className="border-t border-gray-200 p-4 bg-gray-50 flex justify-between items-center">
        <Button 
          variant="outline" 
          onClick={handleGenerateContent}
          className="gap-2"
        >
          <Sparkles size={16} className="text-brand-orange" />
          <span>Generate Visual</span>
        </Button>
        
        <Button 
          onClick={handleSave}
          className="gap-2"
        >
          <Save size={16} />
          <span>Save Note</span>
        </Button>
      </div>
    </div>
  );
};

export default NoteEditor;
