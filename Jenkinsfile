#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2023-03'

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream("knime-database/${env.BRANCH_NAME.replaceAll('/', '%2F')}" +
            ", knime-js-base/${env.BRANCH_NAME.replaceAll('/', '%2F')}")
    ]),
    parameters(workflowTests.getConfigurationsAsParameters() + fsTests.getFSConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

SSHD_IMAGE = "${dockerTools.ECR}/knime/sshd:alpine3.11"

try {
	// Unit tests require an R installation, which is present in a workflow-tests node
    knimetools.defaultTychoBuild('org.knime.update.r', "workflow-tests && maven && java17")

    testConfigs = [
        WorkflowTests: {
            workflowTests.runTests(
                dependencies: [
                    repositories: ['knime-r', 'knime-datageneration', 'knime-js-base',
                        'knime-database', 'knime-office365', 'knime-filehandling', 'knime-kerberos',
                        'knime-exttool', 'knime-chemistry', 'knime-distance',
                        'knime-python-legacy', 'knime-conda']
                ],
                sidecarContainers: [
                    [ image: SSHD_IMAGE, namePrefix: "SSHD", port: 22 ]
                ]
            )
        },
        FilehandlingTests: {
            workflowTests.runFilehandlingTests (
                dependencies: [
                    repositories: [
                        "knime-r", 'knime-database', 'knime-office365', 'knime-kerberos', 'knime-js-base', 'knime-datageneration'
                    ]
                ],
            )
        }
    ]

    parallel testConfigs

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
