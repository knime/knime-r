#!/usr/bin/env bash

chipTypes=""
chipTypes="$chipTypes Test3"
chipTypes="$chipTypes HG-U133_Plus_2"
chipTypes="$chipTypes Mapping10K_Xba142"
chipTypes="$chipTypes Mapping50K_Hind240"
## chipTypes="$chipTypes Mapping50K_Hind240,Xba240"  ## TO UPDATE
chipTypes="$chipTypes Mapping250K_Nsp"
chipTypes="$chipTypes Mapping250K_Sty"
chipTypes="$chipTypes GenomeWideSNP_5"
chipTypes="$chipTypes GenomeWideSNP_6"
chipTypes="$chipTypes Cytogenetics_Array"
chipTypes="$chipTypes CytoScanHD_Array"
chipTypes="$chipTypes MOUSEDIVm520650"
chipTypes="$chipTypes Hs_PromPR_v02"
chipTypes="$chipTypes HuEx-1_0-st-v2"
## chipTypes="$chipTypes MoGene-1_0-st-v1" ## Cannot to GCRMA bg(?!?)

for chipType in ${chipTypes}; do
 echo "nohupR testAll.R --devel --chipTypes ${chipType}"
 nohupR testAll.R --devel --chipTypes ${chipType} &
 sleep 2
done

## nohupR testAll.R --devel --chipTypes Mapping10K_Xba142 &
## nohupR testAll.R --devel --chipTypes Test3 &
## nohupR testAll.R --devel --chipTypes HG-U133_Plus_2 &
## nohupR testAll.R --devel --chipTypes Mapping50K_Hind240,Xba240 &
## nohupR testAll.R --devel --chipTypes Hs_PromPR_v02 &
## nohupR testAll.R --devel --chipTypes Mapping250K_Nsp,Sty &
## nohupR testAll.R --devel --chipTypes HuEx-1_0-st-v2 &
## nohupR testAll.R --devel --chipTypes GenomeWideSNP_5 &
## nohupR testAll.R --devel --chipTypes GenomeWideSNP_6 &
## nohupR testAll.R --devel --chipTypes Cytogenetics_Array &
## nohupR testAll.R --devel --chipTypes CytoScanHD_Array &
