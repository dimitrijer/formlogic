(defn- simplify-tree*
  "Simplifies hiccup format of instaparse parser tree for specified nonterm.
  This is done by removing superfluous levels of nesting from recursive
  elements. For example, [:Conjunction [:Implication [:Term]]] becomes just
  [:Term]."
  [nonterm]
  (fn [& children]
    (if (and (not (contains? excluded-nonterms nonterm))
             (= (count children) 1))
      ;; Return the only child.
      (first children)
      ;; Don't change the node.
      (into [nonterm] children))))

(defn simplify-tree
  "Applies simplify-tree* for all nonterms (except excluded)."
  [tree]
  (instaparse/transform
    ;; This makes a map of :Nonterminal -> (simplify-tree* :Nonterminal).
    (into {} (map #(assoc {} % (simplify-tree* %)) nonterms)) tree))
