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

(defn json-response [data status]
  {:status status
   :body (json/write-str data)}
  )

(defn get-users-boards [user request]
  (->
   {:boards (find-users-boards user)}
   (json-response 200)
   )
  )

(defn post-board [user group request]
  (let [name (:name (json/read-str (slurp (:body request))
                                   :key-fn keyword))]
    (->
     (create-board name group)
     (insert-board)
     (merge {:group (select-keys group [:id :name])})
     (json-response 201)
     )
    )
  )

(defn get-board [user group board request]
  (->
   {}
   (json-response 200)
   )
  )

(defn post-store [user group board request]
  (let [name (:name (json/read-str (slurp (:body request))
                                   :key-fn keyword))]
    (->
     (create-store name board)
     (insert-store)
     (json-response 201)
     )
    )
  )

(defroutes board-routes
  (GET "/boards" request
       (require-login request get-users-boards))

  (GET "/groups/:group-id/boards/:board-id" request
       (require-board request get-board))
  
  (POST "/groups/:group-id/boards" request
        (require-ownership request post-board))

  (POST "/groups/:group-id/boards/:board-id/stores" request
        (require-board request post-store))
  )