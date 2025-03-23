(ns second-brain-studio-webapp.mdx-viewer
  (:require
   [reagent.core :as r]
   [clojure.string :as str]
   ["markdown-it" :as MarkdownIt]
   ["react-syntax-highlighter" :as ReactSyntaxHighlighter]
   ["react-syntax-highlighter/dist/esm/styles/prism" :refer [tomorrow]]
   [second-brain-studio-webapp.ui-generator :as ui-generator]
   [second-brain-studio-webapp.mdx-preview-new :as mdx-preview]))

;; Initialize markdown-it parser with all options enabled
(def md-parser
  (-> (MarkdownIt. #js {:html true
                         :linkify true
                         :typographer true})
      (.enable "table") ;; Enable table parsing
      (.use (fn [md]
              ;; This is where you'd add plugins if needed
              md))))

;; Custom component for code blocks with syntax highlighting
(defn code-block [props]
  (let [{:keys [language value]} (js->clj props :keywordize-keys true)
        SyntaxHighlighter (.-default ReactSyntaxHighlighter)]
    [:> SyntaxHighlighter
     #js {:language (or language "text")
          :style tomorrow
          :customStyle #js {:margin "1em 0"
                           :borderRadius "4px"
                           :padding "1em"}}
     value]))

;; Main MDX viewer component
(defn mdx-viewer
  "Renders markdown content with support for embedded visualizations"
  [props]
  (let [{:keys [content class-name]} props
        processed-content (r/atom "")
        html-content (r/atom "")]
    
    ;; Update content when props change
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [new-content (or (:content (r/props this)) "")]
          ;; Try to use the mdx-preview first, falls back to regular markdown rendering
          (if (and new-content (str/includes? new-content "<VisualComponent"))
            (reset! processed-content [mdx-preview/process-mdx-content new-content])
            (do 
              (reset! processed-content new-content)
              (reset! html-content (.render md-parser new-content))))))
      
      :component-did-update
      (fn [this old-props]
        (let [new-content (or (:content (r/props this)) "")]
          (when (not= new-content (:content old-props))
            ;; Try to use the mdx-preview first, falls back to regular markdown rendering
            (if (and new-content (str/includes? new-content "<VisualComponent"))
              (reset! processed-content [mdx-preview/process-mdx-content new-content])
              (do 
                (reset! processed-content new-content)
                (reset! html-content (.render md-parser new-content)))))))
      
      :reagent-render
      (fn [props]
        (let [{:keys [content class-name]} props]
          [:div.mdx-viewer {:class class-name}
           (if (vector? @processed-content)
             ;; Render content with embedded visualizations
             @processed-content
             
             ;; Render regular markdown content
             [:div.markdown-content 
              {:dangerouslySetInnerHTML {:__html @html-content}}])]))})))

;; Simpler component for just viewing markdown without MDX features
(defn simple-markdown-viewer [content]
  [:div.markdown-content
   {:dangerouslySetInnerHTML 
    {:__html (.render md-parser (or content ""))}}])

;; Example usage in REPL or for testing
(defn example []
  [mdx-viewer {:content "# Hello MDX\n\nThis is a test.\n\n<VisualComponent code=\"%5B%3Adiv.example%20%22This%20is%20a%20sample%20UI%20component%22%5D\" />\n\nMore content after the component."}])

;; Initialize the Markdown parser with options
(def markdown-parser 
  (MarkdownIt. #js {:html true
                    :linkify true
                    :typographer true}))