(ns second-brain-studio-webapp.mdx-preview
  (:require
   [clojure.string :as str]
   [cljs.reader :as reader]
   [reagent.core :as r]))

(defn extract-props [tag]
  (let [props-pattern #"<VisualComponent\s+(.*?)\s*/?>"  ;; Fixed regex to properly escape the closing angle bracket
        props-match (re-find props-pattern tag)
        props-str (when props-match (second props-match))
        props-map (atom {})]
    (if props-str
      (let [prop-matches (re-seq #"(\w+)=[\"'](.*?)[\"']" props-str)]
        (doseq [[_ key value] prop-matches]
          (swap! props-map assoc (keyword key) (str/replace value #"['\"]" "")))
        @props-map)
      {})))

(defn process-mdx-content [content]
  (if (nil? content)
    [:div.empty-content "No content to display"]
    (try
      (let [visual-regex #"<VisualComponent\s+code=\"([^\"]+)\"\s*/?>"
            parts (str/split content visual-regex)]
        (if (= (count parts) 1)
          ;; No embedded visuals found, return content as is
          [:div.markdown-content content]
          ;; Process content with embedded visuals
          (let [processed (atom [(first parts)])
                matches (re-seq visual-regex content)]
            (doseq [[idx [_ code]] (map-indexed vector matches)]
              (let [decoded-code (js/decodeURIComponent code)
                    next-text (get parts (inc (inc idx)) "")]
                (swap! processed conj 
                       [:div.embedded-visual {:key (str "visual-" idx)}
                        (try
                          (let [hiccup (reader/read-string decoded-code)]
                            (if (vector? hiccup)
                              hiccup
                              [:div.error "Invalid Hiccup format"]))
                          (catch js/Error e
                            [:div.error
                             [:h4 "Error parsing code"]
                             [:pre.code-block decoded-code]]))])
                (when-not (empty? next-text)
                  (swap! processed conj next-text))))
            [:div.markdown-with-visuals @processed])))
      (catch js/Error e
        [:div.error
         [:h4 "Error processing MDX content"]
         [:pre.error-message (str e)]]))))

(defn mdx-preview
  "Component to preview MDX content with embedded visualizations"
  [props]
  (let [{:keys [content class-name]} props]
    [:div.mdx-preview {:class class-name}
     [process-mdx-content content]]))