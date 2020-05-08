#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream('knime-database/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
        upstream('knime-js-base/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
    ]),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

try {
	// Unit tests require an R installation, which is present in a workflow-tests node
    knimetools.defaultTychoBuild('org.knime.update.r', "workflow-tests && maven")

    workflowTests.runTests(
        dependencies: [ repositories: ['knime-r', 'knime-datageneration', 'knime-js-base'] ]
    )

    stage('Sonarqube analysis') {
        env.lastStage = env.STAGE_NAME
        workflowTests.runSonar()
    }
} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}
/* vim: set shiftwidth=4 expandtab smarttab: */
