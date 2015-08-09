(ns csp.mockup
  (:require [clojure.core.async :as async :refer :all
             :exclude [map into reduce merge partition partition-by take timeout]]
            [org.httpkit.client :as http]))

(defn report-error [response]
  (println "Error" (:status response) "retrieving URL:" (get-in response [:opts :url])))

(defn http-get-response [ch url]
  (do
    (Thread/sleep 20000)
    (http/get url (fn [response]
                    (if (= 200 (:status response))
                      (put! ch (:body response))
                      (do
                        (report-error response)
                        (close! ch)))))))

(defn crawler-with-timeout
  [url]
  (let [ch (chan)
        t (async/timeout 10000)]
    (do (go
          (alt!
            ch ([x] (println "Read" x "from channel"))
            t (println "Timed out")))
        (http-get-response ch url))))

;;(crawler-with-timeout "https://www.baidu.com/")

(defn retrive
  [func ch]
  (go (Thread/sleep (rand 100))
      (>! ch (func))))

;;返回某一个页面的内容 超时则返回nil
(defn eval-with-timeout
  [func timeout default]
  (let [ch (chan)]
    (retrive func ch)
    (let [[result channel] (alts!! [ch (async/timeout (* timeout 1000))])]
      (if result result default))))

(def url "https://www.baidu.com/")
(def timeout 60)

(eval-with-timeout (fn [] (+ 1 2)) timeout nil)
(eval-with-timeout (fn []
                     @(http/get url (fn [response]
                                      (if (= 200 (:status response))
                                        (:body response)
                                        (str "error retriving -> " url)))))
                   timeout
                   nil)