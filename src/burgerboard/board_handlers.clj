(ns burgerboard.board-handlers
  (:use compojure.core
        burgerboard.authentication
        )
  )

(defn get-boards [user request]
  )

(defn post-board [user request]
  )

(defroutes board-routes
  (GET "/" request
       (require-ownership request get-boards))

  (POST "/" request
        (require-ownership request post-board))
  )