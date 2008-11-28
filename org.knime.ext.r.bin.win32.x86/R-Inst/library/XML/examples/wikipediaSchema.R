
wiki.xsd = xmlSchemaParse("~/Books/XMLTechnologies/Data/Wikipedia/export-0.3.xsd")

names(wiki.xsd)

# wiki.xsd[["name"]]

elems = wiki.xsd$elemDecl
names(elems)

types = wiki.xsd$typeDecl
names(types)
