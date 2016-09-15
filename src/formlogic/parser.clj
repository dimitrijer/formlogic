(ns formlogic.parser
  (:require [instaparse.core :as insta]
            [clojure.tools.logging :as log]
            [clojure.zip :as zip]))

(def logic-parser
  "Main parser for logic expressions."
  (insta/parser (clojure.java.io/resource "logic.bnf")
                :auto-whitespace :standard))

(def nonterms
  "Vector of all non-terminals."
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

(def nonterms-exclude-simplify
  "These nonterms will not be simplified."
  #{:Negation :QuantifierNegation})

(defn- -reconstruct-node
  "Reconstructs node from nonterm key and children."
  [key children]
  (into [key] children))

(defn- -simplify-tree-by
  "Simplifies hiccup format of instaparse parser tree for specified nonterm.
  This is done by removing superfluous levels of nesting from recursive
  elements. For example, [:Disjunction [:Conjunction [:Implication [:Term]]]]
  becomes just [:Term]."
  [key]
  (fn [& children]
    (if (and (not (contains? nonterms-exclude-simplify key))
             (= (count children) 1))
      ;; Return the only child.
      (first children)
      ;; Don't change anything here.
      (-reconstruct-node key children))))

(defn nonterm?
   "Tests if node is a nonterminal."
  ([node]
   (and (vector? node) (keyword? (first node))))
  ([node & nonterm-contents]
     (and (nonterm? node)
          ;; Only compare first N children, where N is number of additional
          ;; args specified.
          (= nonterm-contents (take (count nonterm-contents) node)))))

(defn simplify-tree
  "Applies -simplify-tree-by for all nonterms (except excluded)."
  [tree]
  (insta/transform
    ;; This makes a map of :NonTerminal -> simplify-tree-by-:NonTerminal.
    (into {} (map #(assoc {} % (-simplify-tree-by %)) nonterms)) tree))

(defn- -negate-quantifier
  [quantifier]
  (if (nonterm? quantifier :QuantifierNegation)
    (second quantifier)
    [:QuantifierNegation quantifier]))

(defn- -negate-formula
  [formula]
  (if (nonterm? formula :QuantifiedFormula)
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

(defn- -transform-term-negation
  [& children]
  (let [[[nonterm lhs rhs]] children]
    (case nonterm
      :Conjunction [:Disjunction (-negate-formula lhs) (-negate-formula rhs)]
      :Disjunction [:Conjunction (-negate-formula lhs) (-negate-formula rhs)]
      :Negation lhs
      ;; This is a custom rule, used for ~(\\A x) forms. Replace it with form
      ;; that can be negated in next run with the other rule.
      :QuantifiedFormula [:QuantifiedFormula (-negate-quantifier lhs) rhs]
      ;; In default case, do nothing - reconstruct original node.
      (-reconstruct-node :Negation children))))

(defn- -transform-quantifier-negation
  [& children]
  (let [[quantifier formula] children]
    (if (nonterm? quantifier :QuantifierNegation)
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

(def example-expr "\\A x { Cigla(x) => ((\\E y {Na(x, y) && ~Piramida(y)}) && (~\\E y {Na(x,y) && Na(y,x) }) && (\\A y { ~Cigla(y) => ~Jednako(x,y)}))}")

(defn quantified-formula? [loc] (nonterm? (zip/node loc) :QuantifiedFormula))

(defn- -extract-bound-literal [loc]
  (if (quantified-formula? loc)
    (-> loc zip/down zip/right zip/down zip/right zip/right zip/down zip/right zip/node)
   nil))

(defn- -extract-quantifier [loc]
  (if (quantified-formula? loc)
    (-> loc zip/down zip/right zip/down zip/right zip/down zip/node)
   nil))

(defn next-quantified-formula [loc quantifier]
  (if (zip/end? loc)
    nil
    (if (= quantifier (zip/node loc))
      (-> loc zip/up zip/up zip/up)
      (recur (zip/next loc) quantifier))))

(defn collect-quantifier-bound-atoms
  ([loc] (collect-quantifier-bound-atoms (zip/up loc) []))
  ([loc bound-atoms]
   (if (nil? loc)
     ;; Reached the root.
     bound-atoms
    (recur (zip/up loc)
      (if (and (quantified-formula? loc) (= :FOREACH (-extract-quantifier loc)))
        (conj bound-atoms (-extract-bound-literal loc))
        bound-atoms)))))

(defn replace-literal
  [literal replacement loc]
  (letfn [(recursive? [loc]
            (if (zip/end? loc)
              false
              (if (nonterm? (zip/node loc) :LITERAL literal)
                true
                (recur (zip/next loc)))))
          (alter-subtree [loc]
            (if (zip/end? loc)
              ;; Grab the whole modified subtree.
              (zip/root loc)
              (if (nonterm? (zip/node loc) :LITERAL literal)
                ;; Replace literal.
                (recur (zip/next (zip/edit loc (constantly replacement))))
                ;; No literal here, move on.
                (recur (zip/next loc)))))]
    ;; Check for infinite recursion.
    (if (recursive? (zip/vector-zip replacement))
      (throw (IllegalArgumentException.
               (str "Replacement " replacement " contains literal to replace " literal " !")))
      (zip/edit loc #(alter-subtree (zip/vector-zip %))))))

(def replacement-predicate-index (atom 0))
(defn next-replacement-predicate! []
  (str "F" (swap! replacement-predicate-index + 1)))

(defn transform-existential-quantifiers
  [tree]
  (let [loc (zip/vector-zip tree)
        replace-next-existential-formula
        (fn [loc]
          (if-let [existential-formula (next-quantified-formula loc :EXISTS)]
            (let [bound-literal (-extract-bound-literal existential-formula)
                  bound-atoms (collect-quantifier-bound-atoms existential-formula)
                  replacement (reduce #(conj %1 (vector :LITERAL %2))
                                      [:Predicate [:PRED (next-replacement-predicate!)]]
                                      bound-atoms)
                  ;; Remove the existential formula and replace it with its
                  ;; contents.
                  modified-loc (zip/edit existential-formula
                                         (constantly (-> existential-formula
                                                   zip/down
                                                   zip/right
                                                   zip/right
                                                   zip/node)))]
              ;; We replace all references to the bound-literal with replacement.
              (recur (replace-literal bound-literal replacement modified-loc)))
            ;; Nothing more to do.
            (zip/root loc)))]
    (replace-next-existential-formula loc)))

(defn wff->cnf
  "Converts a well-formed formula in string form to conjuctive-normal-form in
  tree form."
  [formula]
  (-> (logic-parser formula)
      simplify-tree
      transform-implications
      transform-negations
      transform-existential-quantifiers))
