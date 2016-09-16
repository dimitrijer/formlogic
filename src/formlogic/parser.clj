(ns formlogic.parser
  (:require [instaparse.core :as insta]
            [clojure.tools.logging :as log]
            [clojure.zip :as zip]
            [clojure.string :as str]
            [clojure.math.combinatorics :as comb]))

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
  "These nonterms will not be simplified - their forms contain only one child,
  so we would lose them during simplification."
  #{:Negation :QuantifierNegation})

(defn- -construct-node
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
      (-construct-node key children))))

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
      (-construct-node :Negation children))))

(defn- -transform-quantifier-negation
  [& children]
  (let [[quantifier formula] children]
    (if (nonterm? quantifier :QuantifierNegation)
      (let [[[_ foreach-exists literal]] (rest quantifier)]
        (-construct-node :QuantifiedFormula
                           [[:Quantifier
                             (case foreach-exists
                               [:FOREACH] [:EXISTS]
                               [:EXISTS] [:FOREACH]) literal]
                           (-negate-formula formula)]))
      ;; Non-negated quantified formula, do nothing.
      (-construct-node :QuantifiedFormula children))))

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
                                                   ;; Now we are at formula contents.
                                                   zip/node)))]
              ;; We replace all references to the bound-literal with replacement.
              (recur (replace-literal bound-literal replacement modified-loc)))
            ;; Nothing more to do.
            (zip/root loc)))]
    (replace-next-existential-formula loc)))

(defn char-range [start end]
  (map (comp str char) (range (int start) (inc (int end)))))

(def literals (char-range \a \c))

(defn next-unused-literal
  ([used-literals] (next-unused-literal used-literals literals))
  ([used-literals alphabet]
  (let [next-candidate (first (filter #(not (contains? used-literals %)) alphabet))]
    (if (nil? next-candidate)
      (recur used-literals (map str/join (comb/cartesian-product alphabet literals)))
      next-candidate))))

(defn transform-universal-quantifiers
  ([tree] (transform-universal-quantifiers (zip/vector-zip tree) #{}))
  ([loc atoms]
   (if-let [formula (next-quantified-formula loc :FOREACH)]
     (let [literal (-extract-bound-literal formula)
           atoms-with-literal (conj atoms literal)]
       (if (contains? atoms literal)
         ;; This literal was used before. Replace it with unused literal.
         (let [modified-loc (replace-literal literal
                                             [:LITERAL (next-unused-literal atoms)]
                                             formula)]
           (recur (-> modified-loc zip/down zip/right zip/right) atoms-with-literal))
         (recur (-> formula zip/down zip/right zip/right) atoms-with-literal)))
     (zip/root loc))))

(defn- strip-quantified-formula
  ([quantified-formula-loc]
   (if (quantified-formula? quantified-formula-loc)
     (-> quantified-formula-loc
         zip/down
         zip/right
         zip/right
         ;; Now we're at formula contents.
         zip/remove
         ;; Removing puts us at previous depth-first node.
         zip/up
         zip/up
         zip/up)
     ;; Do nothing.
     quantified-formula-loc))
  ([quantified-formula-loc replacement]
   (if (quantified-formula? quantified-formula-loc)
     (-> quantified-formula-loc
         zip/down
         zip/right
         zip/right
         ;; Now we're at formula contents.
         (zip/edit (constantly replacement))
         zip/up)
     ;; Do nothing.
     quantified-formula-loc)))

(defn pull-quantifiers-up
  ([tree] (pull-quantifiers-up (zip/vector-zip tree) [:append-here]))
  ([loc root]
   (if-let [formula (next-quantified-formula loc :FOREACH)]
     (let [modified-formula (-> formula
                                strip-quantified-formula
                                ;; Now we're at formula root. Leave a marker.
                                (zip/append-child [:append-here])
                                zip/node)
           new-root (insta/transform {:append-here (constantly modified-formula)} root)
           ;; Remove the existential formula and replace it with its
           ;; contents.
           modified-loc (zip/edit formula
                                  (constantly (-> formula
                                                  zip/down
                                                  zip/right
                                                  zip/right
                                                  ;; Now we are at formula contents.
                                                  zip/node)))]
       (recur modified-loc new-root))
     ;; Finally, replace the marker at the end of quantifier chain with modified
     ;; tree node, if there are quantifiers at all.
     (if (empty? root)
       (zip/root loc)
       (insta/transform {:append-here (constantly (zip/root loc))} root)))))

(defn- -transform-disjunctions [& children]
  (let [[[lhs-nonterm & lhs-children] [rhs-nonterm & rhs-children]] children
        lhs (first children)
        rhs (second children)]
    (if (= :Conjunction lhs-nonterm)
      ;; Perform left-based descend.
      (let [[conj-lhs-term conj-rhs-term] lhs-children
            left-term (-construct-node :Disjunction [conj-lhs-term rhs])
            right-term (-construct-node :Disjunction [conj-rhs-term rhs])]
        (-construct-node :Conjunction [left-term right-term]))
      (if (= :Conjunction rhs-nonterm)
        ;; Perform right-based descend.
        (let [[conj-lhs-term conj-rhs-term] rhs-children
              left-term (-construct-node :Disjunction [lhs conj-lhs-term])
              right-term (-construct-node :Disjunction [lhs conj-rhs-term])]
          (-construct-node :Conjunction [left-term right-term]))
        ;; Nothing to do here.
        (-construct-node :Disjunction children)))))

(defn descend-disjunctions
  [tree]
  (let [result (insta/transform {:Disjunction -transform-disjunctions} tree)]
    ;; We need to keep doing this until all disjunctions have been dropped down to
    ;; atomics.
    (if (= 0 (compare tree result))
      result
      (recur result))))

(defn skip-quantifiers
  "Skips quantifiers at beginning of loc, returning loc at first non-quantified
  formula."
  [loc]
  (if (quantified-formula? loc)
    (recur (-> loc zip/down zip/right zip/right))
    ;; We're done.
    loc))

(defn extract-quantifiers
  "Extracts quantifiers at beginning of loc, returning loc at start of quantifier
  formula. Also replaces contents with replacement."
  [loc replacement]
  (if-let [previous-loc (zip/up (skip-quantifiers loc))]
    (zip/root (strip-quantified-formula previous-loc replacement))
    []))

(defn filter-quantifiers
  "Returns quantifiers tree without quantifiers that do not bind any literals
  in formula."
  [loc formula]
  (letfn [(extract-literals [loc literals-in-loc]
            (if (zip/end? loc)
              literals-in-loc
              (if (nonterm? (zip/node loc) :LITERAL)
                (recur (zip/next loc) (conj literals-in-loc (-> loc
                                                                zip/down
                                                                zip/right
                                                                zip/node)))
                (recur (zip/next loc) literals-in-loc))))
          (remove-quantifiers [loc literals-in-formula]
            (if-let [next-quantifier (next-quantified-formula loc :FOREACH)]
              (let [bound-literal (-extract-bound-literal next-quantifier)
                    modified-loc (if (not (contains? literals-in-formula bound-literal))
                                   ;; Redundant quantifier, replace quantified
                                   ;; formula with contents.
                                   (zip/edit next-quantifier
                                             (constantly (-> next-quantifier
                                                             zip/down
                                                             zip/right
                                                             zip/right
                                                             ;; Now we are at formula contents.
                                                             zip/node)))
                                   (-> next-quantifier zip/down zip/right zip/right))]
                (recur modified-loc literals-in-formula))
              ;; There are no more quantifiers.
              (zip/root loc)))]
    ;; First extract literals used in formula, then remove all quantifiers that
    ;; do not bind those literals from the loc
    (remove-quantifiers loc (extract-literals (zip/vector-zip formula) #{}))))

(defn collect-conjunction-terms
  "Returns a vector of formulas which are operands to conjunctions, starting at
  loc."
  [loc]
  (let [node (zip/node loc)]
  (if (or (zip/end? loc) (nonterm? node :Conjunction))
    (let [[_ lhs rhs] node
          ;; Collect non-conjunction operands.
          terms (reduce #(conj %1 %2)
                        []
                        (filter #(not (nonterm? % :Conjunction)) [lhs rhs]))]
      ;; Descend into children.
      (concat terms
              (collect-conjunction-terms (-> loc zip/down zip/right))
              (collect-conjunction-terms (-> loc zip/down zip/right zip/right))))
    ;; Not a conjunction.
    [])))

(defn split-on-conjunctions
  [tree]
  (let [loc (zip/vector-zip tree)
        quantifiers (extract-quantifiers loc [:append-here])
        after-quantifiers (skip-quantifiers loc)
        ;; If there are no terms, just return the top formula.
        terms (first (filter (complement empty?)
                             [(collect-conjunction-terms after-quantifiers)
                              [(zip/node after-quantifiers)]]))]
    (if (empty? quantifiers)
      terms
      ;; Filter out redundant quantifiers.
      (map #(filter-quantifiers
              ;; Create a new zipper by appending formula to quantifiers.
              (zip/vector-zip (insta/transform
                                {:append-here (constantly %)}
                                quantifiers))
              %) terms))))

(defn wff->cnf
  "Converts a well-formed formula in string form to conjuctive-normal-form in
  tree form."
  [formula]
  (-> (logic-parser formula)
      simplify-tree
      transform-implications
      transform-negations
      transform-existential-quantifiers
      transform-universal-quantifiers
      pull-quantifiers-up
      descend-disjunctions
      split-on-conjunctions
      ;; From this point on we have a vector of formulas.
      ))
