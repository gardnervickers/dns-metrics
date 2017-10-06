(ns dns-metrics.core
  (:gen-class))

(defn lookup [s]
  (.getHostAddress (java.net.InetAddress/getByName s)))

(defn -main [& args]
  (let [results (atom {})]
    (doseq [addr args]
      (swap!
       results assoc addr
       (try
         (not (clojure.string/blank? (lookup addr)))
         (catch java.net.UnknownHostException e false))))))
