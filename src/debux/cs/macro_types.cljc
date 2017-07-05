(ns debux.cs.macro-types
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [cljs.analyzer.api :as ana]
            [debux.util :as ut] ))

;;; symbol with namespace
(defn ns-symbol [env sym]
  (if-let [meta (ana/resolve env sym)]
    ;; normal symbol
    (let [[ns name] (str/split (str (:name meta)) #"/")]
      ;; The special symbol . must be handled in the following special symbol part.
      ;; However, the special symbol . returns meta {:name / :ns nil}, which may be a bug.
      (if (nil? name)
        sym
        (symbol (str/replace ns "cljs.core" "clojure.core")
                name)))
    ;; special symbol
    sym))


;;; macro management
(def macro-types*
  (atom {:def-type `#{def defonce}
         :defn-type `#{defn defn-}
         :fn-type `#{fn fn*}

         :let-type
         `#{let binding dotimes if-let if-some when-first when-let
            when-some with-out-str with-redefs}
         :letfn-type `#{letfn}
         
         :for-type `#{for doseq}
         :case-type `#{case}

         :skip-arg-1-type `#{set!}
         :skip-arg-2-type `#{as->}
         :skip-arg-1-2-type `#{}
         :skip-arg-2-3-type `#{amap areduce}
         :skip-arg-1-3-type `#{defmethod}
         :skip-form-itself-type
         `#{catch comment declare defmacro defmulti defprotocol defrecord
            deftype extend-protocol extend-type finally import loop memfn new
            quote recur refer-clojure reify var throw}

         :expand-type
         `#{clojure.core/.. -> ->> doto cond-> cond->> condp import some-> some->>}
         :dot-type `#{.} }))


(defn- merge-symbols [old-symbols new-symbols env]
  (->> (map #(ns-symbol env %)
            new-symbols)
       set
       (set/union old-symbols) ))

(defmacro register-macros! [macro-type new-symbols]
  (-> (swap! macro-types* update macro-type
             #(merge-symbols % new-symbols &env))
      ut/quote-vals))

(defmacro show-macros
  ([] (-> @macro-types*
          ut/quote-vals))
  ([macro-type] (-> (select-keys @macro-types* [macro-type])
                    ut/quote-vals)))

