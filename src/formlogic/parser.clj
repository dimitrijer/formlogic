(ns formlogic.parser
  (:require [instaparse.core :as insta]
            [clojure.tools.logging :as log]))

(def logic-parser (insta/parser (clojure.java.io/resource "logic.bnf")
                                :auto-whitespace :standard))

(def nonterms ^{:doc "Vector of all non-terminals"}
  [:WellFormedFormula
   :QuantifiedFormula
   :Quantifier
   :QuantifierNegation
   :Disjunction
   :Conjunction
   :Implication
   :Negation
   :Term
   :Predicate
   :Arguments
   :Argument])

(def nonterms-exclude-simplify #{:Negation :QuantifierNegation})

(defn- -simplify-tree-by
  [key]
  "Simplifies hiccup format of instaparse parser tree for specified nonterm.
  This is done by removing superfluous levels of nesting from recursive
  elements. For example, [:Disjunction [:Conjunction [:Implication [:Term]]]]
  becomes just [:Term]."
  (fn [& children]
    (if (and (not (contains? nonterms-exclude-simplify key))
             (= (count children) 1))
      ;; Return the only child.
      (first children)
      ;; Don't change anything here.
      (into [key] children))))

(defn simplify-tree
  [tree]
  (insta/transform
    ;; This makes a map of :NonTerminal -> simplify-tree-by-:NonTerminal.
    (into {} (map #(assoc {} % (-simplify-tree-by %)) nonterms)) tree))

(defn- -negate-quantifier
  [quantifier]
  (if (= (first quantifier) :QuantifierNegation)
    (second quantifier)
    [:QuantifierNegation quantifier]))

(defn- -negate-formula
  [formula]
  (if (= (first formula) :QuantifiedFormula)
    (let [[_ quantifier formula] formula]
      ;; This will, in turn, negate the formula in next iteration.
      [:QuantifiedFormula (-negate-quantifier quantifier) formula])
    [:Negation formula]))

(defn transform-implications
  [tree]
  (insta/transform
    {:Implication
     (fn [& children]
       (let [[lhs rhs] children]
         [:Disjunction (-negate-formula lhs) rhs]))} tree))

(defn- -reconstruct-node [key children] (into [key] children))

(defn- -transform-term-negation
  [& children]
  (let [[[nonterm lhs rhs]] children]
    (case nonterm
      :Conjunction [:Disjunction (-negate-formula lhs) (-negate-formula rhs)]
      :Disjunction [:Conjunction (-negate-formula lhs) (-negate-formula rhs)]
      :Negation lhs
      ;; In default case, do nothing - reconstruct original node.
      (-reconstruct-node :Negation children))))

(defn- -transform-quantifier-negation
  [& children]
  (let [[quantifier formula] children]
    (if (= (first quantifier) :QuantifierNegation)
      (let [[[_ foreach-exists literal]] (rest quantifier)]
        (-reconstruct-node :QuantifiedFormula
                           [[:Quantifier
                             (case foreach-exists
                               [:FOREACH] [:EXISTS]
                               [:EXISTS] [:FOREACH]) literal]
                           (-negate-formula formula)]))
      ;; Non-negated quantified formula, do nothing.
      (-reconstruct-node :QuantifiedFormula children))))

(defn transform-negations
  [tree]
  (let [result (insta/transform {:Negation -transform-term-negation
                                 :QuantifiedFormula -transform-quantifier-negation} tree)]
    ;; We need to keep doing this until all negations have been dropped down to
    ;; atomics.
    (if (= 0 (compare tree result))
      result
      (recur result))))

(defn depth-tree-seq [tree] (tree-seq #(vector? (second %)) rest tree))

(defn walk-tree
  ([tree f]
   (walk-tree [] tree (constantly true) f))
  ([path tree filter-by f]
   "Walks the syntax tree depth-first, filtering nodes with filter-by, and then
   applying f to matched nodes (f takes path and node)."
   (let [tree-seq (depth-tree-seq tree)
         [subpath matched] (split-with (complement filter-by) tree-seq)
         path (concat path subpath)]
     (f path (first matched))
     (if (empty? (rest matched))
       nil
       (recur path (rest matched) filter-by f)))))

(defn wff->cnf
  [formula]
  "Converts a well-formed formula in string form to conjuctive-normal-form in
  tree form."
  (-> (logic-parser formula)
      simplify-tree
      transform-implications
      transform-negations))

(def example-expr "\\A x { Cigla(x) => ((\\E y {Na(x, y) && ~Piramida(y)}) && (~\\E y {Na(x,y) && Na(y,x) }) && (\\A y { ~Cigla(y) => ~Jednako(x,y)}))}")
