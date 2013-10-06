(ns burgerboard.board-handlers
  (:use compojure.core
        burgerboard.authentication
        )
  )

(defn get-boards [user request]
  {:status 200}
  )

(defn post-board [user group request]
  {:status 201}
  )

(defroutes board-routes
  (GET "/boards" request
       (require-login request get-boards))

  (POST "/groups/:group-id/boards" request
        (require-ownership request post-board))
  )