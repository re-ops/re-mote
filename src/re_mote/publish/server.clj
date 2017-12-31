(ns re-mote.publish.server
  "embedded http server with a websocket for publishing"
  (:require
   [re-share.core :refer (find-port)]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [compojure.core :refer (defroutes GET POST)]
   [compojure.route :as route]
   [hiccup.core :as hiccup]
   [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
   [taoensso.timbre :refer (refer-timbre)]
   [taoensso.sente :as sente]
   [org.httpkit.server :refer (run-server)]
   [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]))

(refer-timbre)

(let [chsk-server (sente/make-channel-socket-server! (get-sch-adapter) {:packer :edn})
      {:keys [ch-recv send-fn connected-uids ajax-post-fn ajax-get-or-ws-handshake-fn]} chsk-server]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids) ; Watchable, read-only atom
)

(defroutes routes
  (route/not-found "<h1>Page not found</h1>"))

(def app
  (wrap-defaults routes site-defaults))

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (trace id event ?data))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server [port]
  (reset! server (run-server #'app {:port port})))

(defn stop  []
  (stop-server))

(defn start []
  (let [p (find-port 8080 9090)]
    (start-server p)))
