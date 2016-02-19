# Changes/Diff Views #

## Description ##

### Change View ###
<p>When writing plugins, particularly those that perform refactoring, it is useful to be able to verify the changes that have been applied to the model.</p>
<p>The change view provides a very straightforward list of changesets and the axioms that have been added and removed.</p>

### Diff View ###
A straightforward view that allows two loaded ontologies to be selected for side-by-side comparison. Each pane displays the axioms that exist in that ontology but not in the other. To use
  * make sure the two ontologies you wish to compare are loaded `*`
  * add the diff view to a tab
  * from the dropdown at the top of each window select the ontologies you are comparing

`*` You can load multiple ontologies into the current workspace by selecting **File | Open** and then selecting **Yes** to open the ontology in the same window. Please not that you cannot currently open two ontologies with the same URI into the same workspace.


## Features ##
  * Changesets are grouped together as they are applied in P4
  * History updates on undo/redo
  * Axiom-level comparison of ontologies

## Screenshots ##

<a href='http://www.co-ode.org/downloads/protege-x/plugins/images/change.png'><img src='http://www.co-ode.org/downloads/protege-x/plugins/images/change.png' width='400' /></a><br /><i>Changes View</i>
<br />

<a href='http://www.co-ode.org/downloads/protege-x/plugins/images/diff-pizza-el.png'><img src='http://www.co-ode.org/downloads/protege-x/plugins/images/diff-pizza-el.png' width='400' /></a><br /><i>Diff View</i>


## Find the plugins ##

Find the Changes and Diff Views under the **Window | Views | Misc views** menu

## Download ##

[download](http://code.google.com/p/co-ode-owl-plugins/downloads/list?q=change)

## Author ##

Nick Drummond, The University of Manchester

## License ##

LGPL