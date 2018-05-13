(ns re-mote.api.server
  "Endpoint api server"
  (:require
   [clojure.core.strint :refer  (<<)]
   [re-share.core :refer (find-port)]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [compojure.core :refer (defroutes GET POST)]
   [compojure.route :as route]
   [hiccup.core :as hiccup]
   [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
   [taoensso.timbre :refer (refer-timbre)]
   [org.httpkit.server :refer (run-server)]
   [ring.middleware.json :refer [wrap-json-response]]
   [ring.util.response :refer [response]]))

(refer-timbre)

(defroutes routes
  (GET "/endpoint/:e" [e]
    (let [f (resolve (symbol (str "re-mote.api.endpoint/" e)))]
      (response (if-not f
                  {:error (<< "endpoint ~{e} not found")}
                  {:result (f)}))))

  (route/not-found "Route not found, try  /endpoint/:e"))

(def app
  (wrap-defaults (wrap-json-response routes) (assoc-in site-defaults [:security :anti-forgery] false)))

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
