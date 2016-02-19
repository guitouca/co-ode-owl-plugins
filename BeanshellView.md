# Beanshell View #

## Description ##
Some coding just doesn't justify setting up an entire java build environment. Some coding requires immediate feedback. Using the Beanshell view, any user can now access the power of the P4 to query and manipulate ontologies or even interact with the UI.

Most of P4 can be accessed from the OWLEditorKit or OWLModelManager (variables eKit and mngr respectively).


## Screenshots ##

<a href='http://www.co-ode.org/downloads/protege-x/plugins/images/P4-scripting-UI.png'><img src='http://www.co-ode.org/downloads/protege-x/plugins/images/P4-scripting-UI.png' width='400' /></a>


## Examples ##

Most of P4 can be accessed from the OWLEditorKit or OWLModelManager (variables '''eKit''' and '''mngr''' respectively).

Here are some code examples you could try.

### Creating a new class in the current ontology ###

```
  ont = mngr.getActiveOntology();
  fac = mngr.getOWLEntityFactory();
  changes = fac.createOWLClass("myNewClass", ont.getURI());
  mngr.applyChanges(changes.getOntologyChanges());
  c = changes.getOWLEntity();
```

### Check if the current ontology is valid OWL DL ###

```
  import  org.semanticweb.owl.profiles.*;
  dlProfile = new OWLDLProfile();
  ont = mngr.getActiveOntology();
  dlProfile.checkOntology(ont, mngr.getOWLOntologyManager());
  report = dlProfile.checkOntology(ont, mngr.getOWLOntologyManager());
  print(report.isInProfile());

  nonDLConstructs = report.getDisallowedConstructs();
  for (c : nonDLConstructs) {
  print(c);
  }
```

### Pick a class to use in a script ###

```
  uiHelper = new UIHelper(eKit);
  c = uiHelper.pickOWLClass();
  print(c);
```

### Use a reasoner ###

First make sure a reasoner has been selected and the ontology is classified.

```
  import org.semanticweb.owl.inference.*;
  r = mngr.getReasoner();
  c = // get your class here - for example as above
  subs = OWLReasonerAdapter.flattenSetOfSets(r.getSubClasses(c));
  print(subs);
```

Note the use of OWLReasonerAdapter as the reasoners return equivalence sets.

### Find all named equivalent classes ###

First make sure a reasoner has been selected and the ontology is classified.

```
  r = mngr.getReasoner();
  onts = mngr.getActiveOntologies();
  for (OWLOntology ont : onts){
    for (OWLClass c : ont.getReferencedClasses()){
      eq = r.getEquivalentClasses(c);
      if (eq.size() > 1){
        print(eq);
      }
    }
  }
```

## Find the plugin ##

The beanshell is a view plugin. Find it under the **Window | Views | Misc views | Beanshell** menu

## Download ##

[download](http://code.google.com/p/co-ode-owl-plugins/downloads/list?can=2&q=beanshell)

## Author ##

P4 plugin developed by Nick Drummond, The University of Manchester

Beanshell is provided by Pat Niemeyer under an SPL or LGPL license. More info available at http://www.beanshell.org/

## License ##

LGPL