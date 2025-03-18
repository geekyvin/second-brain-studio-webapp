(ns second-brain-studio-webapp.sb-backend-client
    (:require
   [reagent.core :as r]
   [clojure.string :as str]))

(defn call-summarize-api [content on-success]
  (-> (js/fetch "http://localhost:3000/get-summary"
                #js {:method "POST"
                     :headers #js {"Content-Type" "application/json"}
                     :body (js/JSON.stringify #js {:content content})})
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error (str "HTTP error! status: " (.-status response)))))))
      (.then (fn [data]
               (js/console.log "API Response:" data)
               (on-success (.-message data))))
      (.catch (fn [error]
                (js/console.error "Error fetching summary:" error)))))

(defn update-summary-section [content summary]
  ;; Find or create the `#### Summary` section
  (let [lines (clojure.string/split content #"\n")
        summary-index (.indexOf lines "#### Summary")
        updated-lines (if (>= summary-index 0)
                        ;; Replace existing summary section
                        (concat (take (inc summary-index) lines) [summary])
                        ;; Append new summary section
                        (concat lines ["#### Summary" summary]))]
    ;; Join the updated lines back into Markdown
    (clojure.string/join "\n" updated-lines)))

(defn call-generate-audio-api [content on-success on-error]
  (-> (js/fetch "http://localhost:3000/generate-audio"
                #js {:method "POST"
                     :headers #js {"Content-Type" "application/json"}
                     :body (js/JSON.stringify #js {:content content})})
      (.then (fn [response]
               (if (.-ok response)
                 (.blob response) ;; Get the audio file as a Blob
                 (throw (js/Error (str "HTTP error! status: " (.-status response)))))))
      (.then (fn [blob]
               (let [audio-url (js/URL.createObjectURL blob)]
                 (on-success audio-url))))
      (.catch (fn [error]
                (js/console.error "Error generating audio:" error)
                (when on-error (on-error error))))))

(defn call-capture-api [file on-success]
  (let [form-data (js/FormData.)]
    (.append form-data "file" file)
    (.append form-data "fileType" (.-type file))
    (-> (js/fetch "http://localhost:3000/capture"
                  #js {:method "POST"
                       :body form-data})
        (.then (fn [response]
                 (if (.-ok response)
                   (.json response)
                   (throw (js/Error. (str "HTTP error! status: " (.-status response)))))))
        (.then (fn [data]
                 (js/console.log "Capture API Response:" data)
                 (on-success data)))
        (.catch (fn [error]
                  (js/console.error "Error capturing file content:" error))))))