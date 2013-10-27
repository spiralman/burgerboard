(ns burgerboard.board-handlers
  (:use compojure.core
        [clojure.data.json :as json :only [read-str write-str]]
        burgerboard.authentication
        burgerboard.database
        burgerboard.boards
        )
  )

(defn require-board [request handler]
  (require-membership
   request
   (fn [user group request]
     (if-let [board (find-group-board group (:board-id (:params request)))]
       (handler user group board request)
       )
     )
   )
  )
    

(defn get-users-boards [user request]
  {:status 200
   :body (json/write-str {:boards (find-users-boards user)})
   }
  )

(defn post-board [user group request]
  (let [name (:name (json/read-str (slurp (:body request))
                                   :key-fn keyword))]
    {:status 201
     :body (json/write-str (merge
                            (insert-board (create-board name group))
                            {:group (select-keys group [:id :name])}
                            ))}
    )
  )

(defn get-board [user group request]
  {:status 200}
  )

(defn post-store [user group board request]
  (let [name (:name (json/read-str (slurp (:body request))
                                   :key-fn keyword))]
    {:status 201
     :body (json/write-str (insert-store (create-store name board)))}
    )
  )

(defroutes board-routes
  (GET "/boards" request
       (require-login request get-users-boards))

  (GET "/groups/:group-id/boards/:board-id" request
       (require-membership request get-board))
  
  (POST "/groups/:group-id/boards" request
        (require-ownership request post-board))

  (POST "/groups/:group-id/boards/:board-id/stores" request
        (require-board request post-store))
  )