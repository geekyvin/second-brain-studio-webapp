(ns second-brain-studio-webapp.sb-backend-client
    (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [second-brain-studio-webapp.subs :as subs]
   [clojure.string :as str]))

;; Helper function to get auth headers
(defn get-auth-headers []
  (let [auth-token @(re-frame/subscribe [::subs/auth-token])]
    (js/console.log "==========================================")
    (js/console.log "CHECKING AUTH TOKEN FOR REQUEST HEADERS")
    (js/console.log "Token from app-db: " (if auth-token "[PRESENT]" "[MISSING]"))
    (when auth-token
      (let [token-preview (if (> (count auth-token) 10)
                          (str (subs auth-token 0 10) "...")
                          auth-token)]
        (js/console.log "Adding Bearer token to request headers:" token-preview)
        (js/console.log "Token length: " (count auth-token))))
    (js/console.log "Local storage token: " (if (.getItem js/localStorage "access_token") "[PRESENT]" "[MISSING]"))
    (js/console.log "Session storage token: " (if (.getItem js/sessionStorage "auth-token") "[PRESENT]" "[MISSING]"))
    (js/console.log "==========================================")
    (cond-> #js {"Content-Type" "application/json"}
      auth-token (js/Object.assign #js {"Authorization" (str "Bearer " auth-token)}))))

(defn call-summarize-api [content on-success]
  (let [headers (get-auth-headers)]
    (-> (js/fetch "http://localhost:3000/get-summary"
                  #js {:method "POST"
                       :headers headers
                       :body (js/JSON.stringify #js {:content content})})
        (.then (fn [response]
                 (if (.-ok response)
                   (.json response)
                   (throw (js/Error (str "HTTP error! status: " (.-status response)))))))
        (.then (fn [data]
                 (js/console.log "API Response:" data)
                 (on-success (.-message data))))
        (.catch (fn [error]
                  (js/console.error "Error fetching summary:" error))))))

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
  (let [headers (get-auth-headers)]
    (-> (js/fetch "http://localhost:3000/generate-audio"
                  #js {:method "POST"
                       :headers headers
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
                  (when on-error (on-error error)))))))

(defn call-capture-api [file on-success]
  (let [form-data (js/FormData.)
        auth-token @(re-frame/subscribe [::subs/auth-token])
        headers (when auth-token #js {"Authorization" (str "Bearer " auth-token)})]
    (.append form-data "file" file)
    (.append form-data "fileType" (.-type file))
    (-> (js/fetch "http://localhost:3000/capture"
                  #js {:method "POST"
                       :headers headers
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

(defn call-generate-ui-api
  "Call the generate-ui API with the given prompt and editor content"
  [prompt editor-content on-success on-error]
  (let [headers (get-auth-headers)
        combined-prompt (str "Markdown content:\n\n" editor-content "\n\nCommand: " prompt)]
    
    (js/console.log "Calling generate-ui API with prompt:" prompt)
    (js/console.log "Combined prompt length:" (count combined-prompt))
    
    (-> (js/fetch "http://localhost:3000/generate-ui"
                 (clj->js {:method "POST"
                          :headers headers
                          :body (js/JSON.stringify #js {:prompt combined-prompt})}))
        (.then (fn [response]
                (if (.-ok response)
                  (.json response)
                  (throw (new js/Error (str "Failed to fetch UI. Status: " (.-status response)))))))
        (.then (fn [data]
                (js/console.log "Generate UI API Response:" data)
                (on-success data)))
        (.catch (fn [error]
                 (js/console.error "Error fetching UI:" error)
                 (when on-error (on-error error)))))))