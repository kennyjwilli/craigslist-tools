(ns craigslist-tools.content-script.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]))

; -- a message loop ---------------------------------------------------------------------------------------------------------

(defn process-message! [message]
  (log "CONTENT SCRIPT: got message:" message))

(defn run-message-loop! [message-channel]
  (log "CONTENT SCRIPT: starting message loop...")
  (go-loop []
           (when-some [message (<! message-channel)]
             (process-message! message)
             (recur))
           (log "CONTENT SCRIPT: leaving message loop")))

; -- a simple page analysis  ------------------------------------------------------------------------------------------------

(defn do-page-analysis! [background-port]
  (let [script-elements (.getElementsByTagName js/document "script")
        script-count (.-length script-elements)
        title (.-title js/document)
        msg (str "CONTENT SCRIPT: document '" title "' contains " script-count " script tags.")]
    (log msg)
    (post-message! background-port msg)))

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    (post-message! background-port "hello from CONTENT SCRIPT!")
    (run-message-loop! background-port)
    (do-page-analysis! background-port)))

; -- craig stuff  ------------------------------------------------------------------------------------------------

(defonce state (atom {:banish-index 0}))

(defn get-card-elem
  [idx]
  (aget (.querySelectorAll js/document ".result-row:not(.banished)") idx))

(defn banish-item!
  [idx]
  (log "banish" idx)
  (.. (get-card-elem idx) (querySelector ".banish") click))

(defn update-item-card
  [index-cur index-next]
  (let [set-bg-color (fn [elem bg-val]
                       (set! (.. elem -style -backgroundColor) bg-val))
        card-cur (get-card-elem index-cur)
        card-next (get-card-elem index-next)]
    (set-bg-color card-cur "")
    (set-bg-color card-next "antiquewhite")))

(defn change-banish-index!
  [index-or-fn]
  (swap! state update :banish-index
         (fn [idx-cur]
           (let [idx-next (if (fn? index-or-fn)
                            (index-or-fn idx-cur)
                            index-or-fn)]
             (update-item-card idx-cur idx-next)
             idx-next))))

(defn on-keydown
  [evt]
  (let [node-name (.. evt -target -nodeName)]
    (when-not (or (= node-name "INPUT") (= node-name "TEXTAREA"))
      (let [key-code (.-keyCode evt)]
        (case key-code
          ;; del or backspace key
          (8 46)
          (let [idx (:banish-index @state)]
            (banish-item! idx)
            (change-banish-index! idx))
          ;; left arrow
          37
          (change-banish-index! dec)
          ;; right arrow
          39
          (change-banish-index! inc)
          nil)))))

(defn add-keydown-listener
  []
  (.addEventListener js/document "keydown" on-keydown false))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (log "CONTENT SCRIPT: init")
  (change-banish-index! 0)
  (add-keydown-listener)
  (connect-to-background-page!))
