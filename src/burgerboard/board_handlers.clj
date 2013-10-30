(ns burgerboard.board-handlers
  (:use compojure.core
        [clojure.string :only [join]]
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

(defn require-store [request handler]
  (require-board
   request
   (fn [user group board request]
     (if-let [store (find-board-store board (:store-id (:params request)))]
       (handler user group board store request)
       )
     )
   )
  )

(defn resolve-route [request & route]
  (join "/"
        (concat
         [(str (-> request :scheme name)
               "://"
               (get-in request [:headers "host"]))
          "api"
          "v1"]
         route)
        )
  )

(defn json-response [data status]
  {:status status
   :body (json/write-str data)}
  )

(defn get-users-boards [user request]
  (->
   {:boards
    (map
     (fn [board]
       (assoc board
         :url (resolve-route request
                             "groups" (:id (:group board)) "boards" (:id board)))
       )
     (find-users-boards user))}
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
   (find-board-ratings board)
   (tally-board board)
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

(defn put-rating [user group board store request]
  (let [rating (int (:rating (json/read-str (slurp (:body request))
                                            :key-fn keyword)))]
    (->
     (set-rating store user rating)
     (tally-store)
     (json-response 200)
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

  (PUT "/groups/:group-id/boards/:board-id/stores/:store-id/rating" request
       (require-store request put-rating))
  )