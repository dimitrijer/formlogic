(ns formlogic.parser
  (:require [instaparse.core :as insta]
            [clojure.tools.logging :as log]))

(def logic-parser (insta/parser (clojure.java.io/resource "logic.bnf")
                                :auto-whitespace :standard))

(def nonterms ^{:doc "Vector of all non-terminals"}
  [:WellFormedFormula
   :Quantifiers
   :Quantifier
   :Disjunction
   :Conjunction
   :Implication
   :Negation
   :Term
   :Predicate
   :Arguments
   :Argument])

(defn simplify-tree-by
  [key]
  "Simplifies hiccup format of instaparse parser tree for specified nonterm.
  This is done by removing superfluous levels of nesting from recursive
  elements. For example, [:Disjunction [:Conjunction [:Implication [:Term]]]]
  becomes just [:Term]."
  (fn [& children]
    (if (= (count children) 1)
      ;; Return the only child.
      (first children)
      ;; Don't change anything here.
      (into [key] children))))

(defn simplify-tree
  [tree]
  (insta/transform
    ;; This makes a map of :NonTerminal -> simplify-tree-by-:NonTerminal.
    (into {} (map #(assoc {} % (simplify-tree-by %)) nonterms)) tree))

(defn transform-implications
  [tree]
  (insta/transform
    {:Implication
     (fn [& children]
       (let [[lhs rhs] children]
         [:Disjunction [:Negation "~" lhs] rhs]))} tree))

(defn transform-negations
  [tree]
  (let [transform-collection
        (fn [& children]
          (let [[_ term] children
                [nonterm lhs rhs] term]
            (case nonterm
              :Conjunction [:Disjunction [:Negation "~" lhs] [:Negation "~" rhs]]
              :Disjunction [:Conjunction [:Negation "~" lhs] [:Negation "~" rhs]]
              :Negation rhs
              ;; Default case.
              (into [:Negation] children))))
        result (insta/transform {:Negation transform-collection} tree)]
    ;; We need to keep doing this until all negations have been dropped down to
    ;; literals.
    (if (= 0 (compare tree result))
      result
      (recur result))))

(defn walk-tree
  [tree f]
  "Walks the syntax tree depth-first, applying f to leaves of the tree."
  (apply f (filter string? (tree-seq #(not (string? %)) rest tree))))

(defn wff->cnf
  [formula]
  "Converts a well-formed formula to conjuctive-normal-form."
  (-> (logic-parser formula)
      simplify-tree
      transform-implications
      transform-negations))
