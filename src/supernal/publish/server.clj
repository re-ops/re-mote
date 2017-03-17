(ns supernal.publish.server
  "embedded http server with a websocket for publishing"
  (:require
    [supernal.publish.index :refer (index)]
    [clojure.string     :as str]
    [ring.middleware.webjars :refer [wrap-webjars]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [compojure.core     :as comp :refer (defroutes GET POST)]
    [compojure.route    :as route]
    [hiccup.core        :as hiccup]
    [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
    [taoensso.timbre    :as timbre :refer (tracef debugf infof warnf errorf)]
    [taoensso.sente     :as sente]
    [org.httpkit.server :as http-kit :refer (run-server)]
    [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]))


(let [chsk-server (sente/make-channel-socket-server! (get-sch-adapter) {:packer :edn})
     {:keys [ch-recv send-fn connected-uids ajax-post-fn ajax-get-or-ws-handshake-fn]} chsk-server]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv) 
  (def chsk-send! send-fn)
  (def connected-uids connected-uids) ; Watchable, read-only atom
  )

;; We can watch this atom for changes if we like
(add-watch connected-uids :connected-uids
  (fn [_ _ old new]
    (when (not= old new)
      (infof "Connected uids change: %s" new))))

(defroutes routes
  (GET  "/"      ring-req (index))
  (GET  "/chsk"  ring-req (ring-ajax-get-or-ws-handshake ring-req))
  (POST "/chsk"  ring-req (ring-ajax-post ring-req))
  (route/files "/" {:root "public"})
  (route/not-found "<h1>Page not found</h1>"))

(def app
  (wrap-defaults (wrap-webjars routes) site-defaults))

(defn test-fast-server>user-pushes
  "Quickly pushes 100 events to all connected users. Note that this'll be fast+reliable even over Ajax!"
  []
  (doseq [uid (:any @connected-uids)]
    (doseq [i (range 100)]
      (chsk-send! uid [:fast-push/is-fast (str "hello " i "!!")]))))

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (println id event ?data))

(defonce router (atom nil))

(defn  stop-router []
  (when-let [stop-fn @router] (stop-fn)))

(defn start-router []
  (stop-router)
  (reset! router
    (sente/start-server-chsk-router! ch-chsk event-msg-handler)))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server [port]
  (reset! server (run-server #'app {:port port})))

(defn broadcast! [m]
  (let [uids (:any @connected-uids)]
    (debugf "Broadcasting server>user: %s uids" (count uids))
    (doseq [uid uids] (chsk-send! uid m))))

(defn stop  []
  (stop-router)
  (stop-server))

(defn start []
  (start-router)
  (start-server 8080))



(comment
  (require 'clojure.data.json)

  (binding [*out* (clojure.java.io/writer "out.edn")]
    (clojure.pprint/pprint (clojure.data.json/read-str (slurp "stock.json") :key-fn keyword)))

  (defn linear-values [s]
    (mapv (fn [i] {:x i :y (rand-int 1000) }) (range s)))

  (broadcast! [::vega {:values (linear-values 20) :graph :vega/lines}])

  (defn grouped-values [s c]
    (map (fn [i] {:x i :y (rand-int 10) :c (+ 1 c)}) (range s)))

  (broadcast! [::vega {:values (mapcat (partial grouped-values 20) (range 4)) :graph :vega/stack}])

  (defn grouped-dates [s host]
    (map (fn [i] {:x (+ (* i 1000) 1489675925417) :y (rand-int 10) :host host}) (range s)))

  (use 'supernal.repl.schedule)
  (watch 10 
    (fn [] (broadcast! [::vega {:values (mapcat (partial grouped-dates 20) [:supa :supb :supc :supd]) :graph :vega/stock}])))
  
)
