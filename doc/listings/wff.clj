(defn wff->cnf
  "Converts a well-formed formula in string form to
  conjuctive-normal-form in tree form."
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
      rename-bound-vars
      remove-quantifiers))
