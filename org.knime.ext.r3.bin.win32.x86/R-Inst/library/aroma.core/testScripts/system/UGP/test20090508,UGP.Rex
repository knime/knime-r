if (interactive()) savehistory();
library("aroma.core");

verbose <- Arguments$getVerbose(-8, timestamp=TRUE);

chipType <- "Mapping10K_Xba142";
ugp <- AromaUgpFile$byChipType(chipType);
print(ugp);

chromosomes <- getChromosomes(ugp);
print(chromosomes);

units <- getUnitsOnChromosome(ugp, chromosome=13);
str(units);

chromosomes <- seq(from=1,to=23,by=2);
unitsList <- getUnitsOnChromosomes(ugp, chromosomes=chromosomes);
str(unitsList);

units <- getUnitsOnChromosomes(ugp, chromosomes=chromosomes, 
                               unlist=TRUE, useNames=FALSE);
str(units);

dataList <- extractByChromosome(ugp, chromosomes=chromosomes);
str(dataList);
