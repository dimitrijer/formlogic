# formlogic

This is an app that I'm building for my bachelor thesis. I also use it as an
excuse to learn Clojure - sorry for my newb coding!

## Grammar description

Logic grammar building blocks are *atoms*, *predicates*, *operators* and
*quantifiers*.

**Atoms** are represented as sequences of lowercase alphabet letters, for example
`abc` or `x`. **Predicates** are represented as sequences of lowercase letters that
begin with a capital letter, e.g. `Predicate`.

Logic grammar has support for the following logic **operators**, listed here by
implicit order of precedence, from most immediate to least:

1. _Implication_ denoted as `=>`
2. _Negation_ as `~`
3. _Conjunction_ as `&&`
4. _Disjunction_ as `||`

Implicit precedence of operators can be overriden by using parenthesis (`(` and `)`).

The following logic quantifiers are supported:

* _For each_ as `\A`
* _Exists_ as `\E`

Logic quantifiers are applied to first logic expression in curly braces after
the quantifier.

An example of logic expression:
```
\A x {Cigla(x) => ((\E y {Na(x, y) && ~Piramida(y)}) && (~\E y {Na(x,y) && Na(y,x)}) && (\A y {~(Cigla(y)) => ~Jednako(x,y)}))}
```

Note that, when used as operands to logic operators, logic quantifiers and
their expressions *must be* surrounded in parenthesis. This is needed in order to
make the grammar unambiguous.

## Usage

Build and run with `lein run`. Or you can package it in a uberjar and run it
manually:

    $ java -jar formlogic-0.1.0-standalone.jar [args]

## Examples

...

### Bugs

...

### Nice To Have
* Remember Me functionality (via cookies)
* Log each request start and end, and keep started time in session.
### That You Think
### Might be Useful

## License

Copyright © 2016 Dimitrije Radojević

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
