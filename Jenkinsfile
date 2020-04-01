#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
    // provide a list of upstream jobs which should trigger a rebuild of this job
    pipelineTriggers([
        upstream('knime-database/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
        upstream('knime-js-base/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
    ]),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

try {
    knimetools.defaultTychoBuild('org.knime.update.r')

    // Specifying configurations is optional. If omitted, the default configurations will be used
    // (see jenkins-pipeline-libraries/vars/workflowTests.groovy)
    // def testConfigurations = [
    //     "ubuntu18.04 && python-3",
    //     "windows && python-3"
    // ]

    workflowTests.runTests(
        dependencies: [
            // All features (not plug-ins!) in the specified repositories will be installed.
            repositories: ['knime-r'],
            // an optional list of additional bundles/plug-ins from the repositories above that must be installed
            // ius: ['org.knime.json.tests']
        ],
        // this is optional and defaults to false
        withAssertions: true,
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

/* vim: set ts=4: */
