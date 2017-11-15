## simmap NeXML definitions 

- Author: Carl Boettiger
- Initial version: 2014-03-21



Definitions of the `simmap` namespace, as defined for the use in `RNeXML`. The prefix `nex:` refers to the [NeXML schema](http://www.nexml.org/2009).


  term               | definition
 ------------------- | -------------
 `simmap:reconstructions`   | A container of one or more stochastic character map reconstructions, as a `meta` child of a the `nex:edge` element to which the contained stochastic character map reconstructions are being assigned.
 `simmap:reconstruction`    | A single stochastic character map reconstruction for a given `nex:edge`. Normally nested within a `simmap:reconstructions` element.
 `simmap:char`              | The id of a character trait, as defined by the `nex:char` element with this value as its `id`. This is a property of a `simmap:reconstruction`.
 `simmap:stateChange`       | A character state assignment to the given `nex:edge` during a specified interval, as a property of a `simmap:reconstruction`. Must have children `simmap:order`, `simmap:length`, and `simmap:state`.
`simmap:order`              | The chronological order (from the root) in which the state is assigned to the edge.  An edge that does not change states still has `simmap:order` 1.   This is a property of a `simmap:stateChange`.  
`simmap:length`             | The duration for which the edge occupies the assigned state, in the same units as the `nex:length` attribute defined on the `nex:edge` being annotated. This is a property of a `simmap:stateChange`.  
 `simmap:state`             | The id of a `nex:state` of the `nex:char` identified by the `simmap:char` property of the `simmap:reconstruction`. This is a property of a `simmap:stateChange`.  
