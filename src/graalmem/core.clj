(ns graalmem.core
  (:gen-class)
  (:require
    [aero.core :as aero]
    [clojure.java.io :as jio]
    [prism.core :as prism]
    [prism.http-server :as hs]
    [prism.json :as json]
    [ring.adapter.jetty9 :as jetty])
  (:import
    (org.eclipse.jetty.server Server)))

(def handler
  (hs/create-handler
    [["/" {:get {:handler (constantly {:status 200})}}]
     ["/proxy*path" {:get {:handler (fn proxy-echo [req]
                                      (println "proxy echo:" req)
                                      {:status 200
                                       :body   {:body (:body-params req)
                                                :path (-> req :path-params :path)}})}}]
     ["/echo"
      ["/formatted" {:get {:handler (fn proxy-echo [req]
                                      (println "formatted:" req)
                                      {:status  200
                                       :headers {"Content-Type" "application/json"}
                                       :body    (-> (:body-params req)
                                                    (json/write-json-string))})}}]]]))

(defn configure-server [^Server server]
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(do
                                (println "Stopping server")
                                (.stop server)
                                (println "Server stopped")))))

(defn- register-deep-merge! []
  (letfn [(reconcile-keys [val-in-result val-in-latter]
            (if (and (map? val-in-result)
                     (map? val-in-latter))
              (merge-with reconcile-keys val-in-result val-in-latter)
              val-in-latter))
          (reconcile-maps [result latter]
            (merge-with reconcile-keys result latter))
          (deep-merge [& maps]
            (reduce reconcile-maps maps))]
    (defmethod aero/reader 'deep-merge
      [_ _ value]
      (apply deep-merge value))))

(defn start! [port]
  (register-deep-merge!)
  (println (format "Starting http server on port: %s" port))
  (-> (jio/resource "config.edn") aero/read-config str) ;magic line
  (println (format "Config: %s" (prism/config)))
  (json/configure-logging!)
  (jetty/run-jetty
    handler
    {:port         port
     :join?        false
     :h2c?         true
     :configurator configure-server}))

(defn -main [& _]
  (start! 3001))
