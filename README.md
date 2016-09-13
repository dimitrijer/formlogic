# formlogic

## Grammar description

Logic grammar building blocks are *atoms*, *predicates*, *operators* and
*quantifiers*.

**Atoms** are represented as single alphabet letters, for example `a` or `x`.
**Predicates** are represented as words beginning with a capital letter, e.g. `Predicate`.

Logic grammar has support for the following logic **operators**, listed here by
implicit order of precedence, from most immediate to least:
1. _Implication_ denoted as `=>`
2. _Negation_ as `~`
3. _Conjunction_ as `&&`
4. _Disjunction_ as `||`
Implicit precedence of operators can be overriden by using parenthesis (`(` and `)`).

The following logic quantifiers are supported:
* _For each_ \A
* _Exists_ \E
Logic quantifiers are applied to first logic expression in curly braces after
the quantifier.

An example of logic expression:
```
\A x {Cigla(x) => ((\E y {Na(x, y) && ~Piramida(y)}) && (~\E y {Na(x,y) && Na(y,x)}) && (\A y {~(Cigla(y)) => ~Jednako(x,y)}))}
```

Note that, when used as operands to logic operators, logic quantifiers and
their expressions *must be* surrounded in parenthesis. This is needed in order to
make the grammar unambiguous.

## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar formlogic-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Nice To Have
* Remember Me functionality (via cookies)
### That You Think
### Might be Useful

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
