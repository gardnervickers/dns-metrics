(ns dns-metrics.core
  (:gen-class)
  (:require [prometheus.core :as prometheus]
            [taoensso.timbre :as timbre]
            [ring.server.standalone :refer [serve]]))

(defn lookup [s]
  (.getHostAddress (java.net.InetAddress/getByName s)))

(defn register-url-metric [store]
  (prometheus/register-counter store
                               "dns_metrics"
                               "dns_failures"
                               "DNS resolution failure count"
                               ["URL"]))

(defn increase-error-count
  ([store url]
   (increase-error-count store url 1))
  ([store url ammount]
   (prometheus/increase-counter store "dns_metrics" "dns_failures"
                                [(clojure.string/replace url #"\." "_")] ammount)))

(defn -main [& args]
  (timbre/info "Starting monitoring for: " args)
  (let [store (register-url-metric (prometheus/init-defaults))]
    (doseq [addr args]
      (timbre/info "Initializing counter for: " addr)
      (increase-error-count store addr 0))
    (future
      (while true
        (doseq [addr args]
          (timbre/info "Checking DNS resolution for: " addr)
          (when-not (try
                      (not (clojure.string/blank? (lookup addr)))
                      (catch java.net.UnknownHostException e false))
            (increase-error-count store addr)))
        (java.util.concurrent.locks.LockSupport/parkNanos 5e+9)))
    (serve
     (prometheus/instrument-handler (fn [_]
                                      (prometheus/dump-metrics (:registry store)))
                                    "dns_error_metrics"
                                    (:registry store)))))

;;(-main "foobar" "www.google.com" "localhost" "localhow")
