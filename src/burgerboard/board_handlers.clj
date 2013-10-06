(ns burgerboard.board-handlers
  (:use compojure.core
        [clojure.data.json :as json :only [read-str write-str]]
        burgerboard.authentication
        burgerboard.database
        )
  )

(defn get-boards [user request]
  {:status 200
   :body (json/write-str {:boards (find-users-boards user)})
   }
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