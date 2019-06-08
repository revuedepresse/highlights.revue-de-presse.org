; About gen-class examples
; @see https://clojure.org/reference/compilation#_gen_class_examples
(ns review.core
  (:require [clojure.tools.logging :as log])
  (:use [korma.db]
        [twitter.api-client]
        [amqp.message-handler]
        [command.generate-keywords]
        [command.generate-timely-statuses]
        [command.update-members-props]
        [command.save-highlights]
        [command.recommend-subscriptions]
        [command.unarchive-statuses])
  (:gen-class))

(log/log-capture! "review")

(defn execute-command
  [name args]
  (cond
     (= name "consume-amqp-messages")
       (let [[queue messages consumers] args
             total-messages (if (nil? messages)
                              100
                              (Long/parseLong messages))
             parallel-consumers (if (nil? consumers)
                                  1
                                  (Long/parseLong consumers))]
         (try
           (consume-messages (keyword queue) total-messages parallel-consumers)
           (catch Exception e (log/error
                                (str "An error occurred with message: " (.getMessage e))))))
     (= name "recommend-subscriptions")
       (let [[screen-name] args]
         (recommend-subscriptions-from-member-subscription-history screen-name))
     (= name "update-members-descriptions-urls")
       (update-members-descriptions-urls)
     (= name "unarchive-statuses")
       (let [[week year] args
             year (Long/parseLong year)
             week (Long/parseLong week)]
         (unarchive-statuses week year))
     (= name "generate-timely-statuses")
       (let [[week year] args
             year (Long/parseLong year)
             week (Long/parseLong week)]
         (generate-timely-statuses week year))
     (= name "generate-timely-statuses-for-member")
       (let [[member year] args
             year (Long/parseLong year)]
         (generate-timely-statuses-for-member member year))
     (= name "generate-keywords-from-statuses")
       (let [[date] args]
         (if (> (count args) 1)
           (generate-keywords-for-all-aggregates date
                                                 {:week (Long/parseLong (first args))
                                                  :year (Long/parseLong (second args))})
           (generate-keywords-for-all-aggregates date)))
     (= name "record-popularity-of-highlights")
       (let [[date] args]
         (record-popularity-of-highlights date))
     (= name "save-highlights")
       (let [[date] args]
         (cond
           (nil? date)
            (save-today-highlights)
           (= 0 (count args))
            (save-highlights date)
           :else
            (apply save-highlights args)))
     (= name "save-highlights-for-all-aggregates")
       (let [[date] args]
          (save-highlights-for-all-aggregates date))
     :else
       (log/info "Invalid command")))

(defn -main
  "Command dispatch application (AMQP message consumption / recommendation)"
  [name & args]
  (try
    (execute-command name args)
    (catch Exception e (log/error (.getMessage e)))))

