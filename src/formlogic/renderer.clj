(ns formlogic.renderer
  (:require [clojure.zip :as zip]
            [clojure.walk :refer (postwalk)]
            [formlogic.parser :as parser]))

(def ^:private nonterm->latex
  {:Disjunction "\\lor"
   :Conjunction "\\land"
   :Implication "\\Rightarrow"})

(defn hiccup->latex [node]
  {:pre [(parser/nonterm? node)]}
  (let [[nonterm lhs rhs] node]
    (case nonterm
      (:PRED :LITERAL) (str lhs)
      :FOREACH "\\forall"
      :EXISTS "\\exists"
      (:QuantifierNegation
        :Negation) (str "\\neg " (hiccup->latex lhs))
      :Predicate (str (hiccup->latex lhs)
                      "("
                      ;; Separate arguments with commas.
                      (apply str (interpose "," (map hiccup->latex (next (next node)))))
                      ")")
      :QuantifiedFormula (apply str (interpose " " (vector
                                                     (hiccup->latex lhs)
                                                     "\\{"
                                                     (hiccup->latex rhs)
                                                     "\\}")))
      (:Disjunction
        :Conjunction
        :Implication
        :Quantifier) (apply str (interpose " " (remove nil?
                                                       (vector
                                                         (hiccup->latex lhs)
                                                         (nonterm nonterm->latex)
                                                         (hiccup->latex rhs))))))))

