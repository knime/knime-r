z <- xmlTree("moby:MOBY", namespaces = list(moby = "http://www.biomoby.org/moby"))
 z$setNamespace("moby")
 z$addNode("mobyContent", close = FALSE)
 z$addNode("mobyData", attrs = c("moby:queryID" = "sip_1_"), close = FALSE)
 z$addNode("Simple", attrs = c("moby:articleName" = "accession"), close = FALSE)
 z$addNode("Global_Keyword", attrs = c("moby:id" = "PB000122", "moby:namespace" = ""))
 z$closeTag()
 z$closeTag()
 z$closeTag()
 z$closeTag()


zz <- newXMLNode("moby:MOBY", namespaceDefinitions = list(moby = "http://www.biomoby.org/moby"))
addChildren(zz, ctnt <- newXMLNode("mobyContent"))
addChildren(ctnt, data <- newXMLNode("mobyData", attrs = c("moby:queryID" = "sip_1_")))
addChildren(data, simple <- newXMLNode("Simple", attrs = c("moby:articleName" = "accession")))
addChildren(simple, newXMLNode("Global_Keyword", attrs = c("moby:id" = "PB000122", "moby:namespace" = "")))



