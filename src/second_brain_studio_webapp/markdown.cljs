(ns second-brain-studio-webapp.markdown
  (:require ["marked" :as marked]
            ["prismjs" :as Prism]))
  
  (defn render-markdown [content]
    [:div {:dangerouslySetInnerHTML
           {:__html (marked/parse content)}}])
  

  (defn highlight-code [html]
    (let [highlighted (. Prism highlight html "markdown")]
      {:dangerouslySetInnerHTML {:__html highlighted}}))
  
  (defn auto-link [content]
    (str/replace content #"\[\[(.*?)\]\]"
                 (fn [[_ text]]
                   (str "<a href='#" (str/lower-case text) "'>" text "</a>"))))
  
  (defn handle-shortcut [event]
    (when (and (.-ctrlKey event) (= (.-key event) "b"))
      (.preventDefault event)
      (let [input (.-target event)
            start (.-selectionStart input)
            end (.-selectionEnd input)
            value (.-value input)]
        (set! (.-value input)
              (str (subs value 0 start) "**" (subs value start end) "**" (subs value end)))
        (.setSelectionRange input (+ start 2) (+ end 2)))))

  