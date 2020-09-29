#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream("knime-database/${env.BRANCH_NAME.replaceAll('/', '%2F')}" +
            ", knime-js-base/${env.BRANCH_NAME.replaceAll('/', '%2F')}")
    ]),
    parameters(workflowTests.getConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

SSHD_IMAGE = "${dockerTools.ECR}/knime/sshd:alpine3.10"

try {
	// Unit tests require an R installation, which is present in a workflow-tests node
    knimetools.defaultTychoBuild('org.knime.update.r', "workflow-tests && maven")

    workflowTests.runTests(
        dependencies: [
            repositories: ['knime-r', 'knime-datageneration', 'knime-js-base',
                'knime-database', 'knime-filehandling', 'knime-kerberos',
                'knime-exttool', 'knime-chemistry', 'knime-distance']
        ],
        sidecarContainers: [
            [ image: SSHD_IMAGE, namePrefix: "SSHD", port: 22 ]
        ]
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
