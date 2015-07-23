// this code was generated using the rkwarddev package.
//perhaps don't make changes here, but in the rkwarddev script instead!



function preprocess(){
	// add requirements etc. here
	echo("require(cocron)\n");
}

function calculate(){
	// read in variables from dialog
	var groups = getValue("groups");
	var dataInput = getValue("data_input");
	var rawDataAlpha = getValue("raw_data_alpha");
	var alphaCount = getValue("alpha_count");
	var manualAndDepGroupsN = getValue("manual_and_dep_groups_n");
	var manualAndDepGroupsI = getValue("manual_and_dep_groups_i");
	var manualAndIndepGroupsAlpha = getValue("manual_and_indep_groups_alpha");
	var manualAndIndepGroupsN = getValue("manual_and_indep_groups_n");
	var manualAndIndepGroupsI = getValue("manual_and_indep_groups_i");
	var manualAndDepGroupsAlpha = getValue("manual_and_dep_groups_alpha");
	var manualAndDepGroupsR = getValue("manual_and_dep_groups_r");
	var standardizedAlpha = getValue("standardized_alpha");
	var los = getValue("los");
	var confInt = getValue("conf_int");

	// the R code to be evaluated
	if(dataInput == 'raw.data') {
		echo("result <- cocron(");
	}
	var rawDataAlpha = getValue("raw_data_alpha").split("\n").join(", ");
	if(dataInput == 'raw.data' && groups == 'indep') {
		echo("data=list(" + rawDataAlpha + "), dep=FALSE");
	}
	if(dataInput == 'raw.data' && groups == 'dep') {
		echo("data=list(" + rawDataAlpha + "), dep=TRUE");
	}
	if(dataInput == 'raw.data' && standardizedAlpha == 'true') {
		echo(", standardized=TRUE");
	}
	if(dataInput == 'manual') {
		echo("result <- cocron.n.coefficients(");
	}
	if(dataInput == 'manual' && groups == 'indep') {
		echo("alpha=as.vector(" + manualAndIndepGroupsAlpha + "), n=as.vector(" + manualAndIndepGroupsN + "), items=as.vector(" + manualAndIndepGroupsI + "), dep=FALSE");
	}
	if(dataInput == 'manual' && groups == 'dep') {
		echo("alpha=as.vector(" + manualAndDepGroupsAlpha + "), n=" + manualAndDepGroupsN + ", items=" + manualAndDepGroupsI + ", dep=TRUE, r=" + manualAndDepGroupsR);
	}
	echo(", los=" + los + ", conf.level=" + confInt + ")\n");
}

function printout(){
	// printout the results
	echo("rk.header(\"Comparing Cronbach alphas\")\n");

	echo("rk.print(result)\n");

}

