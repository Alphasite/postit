void setBuildStatus(String message, String state) {
  step([
      $class: "GitHubCommitStatusSetter",
      reposSource: [$class: "ManuallyEnteredRepositorySource", url: "https://github.com/my-org/my-repo"],
      contextSource: [$class: "ManuallyEnteredCommitContextSource", context: "ci/jenkins/build-status"],
      errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
  ]);
}

pipeline {
    agent any

    stages {
        stage('Preparation') {
            steps {
                checkout scm
                sh "rm -f pit-test.zip"
                sh "rm -f coverage.zip"
            }
        }

        stage('Build') {
            steps {
                sh "./gradlew clean assemble"
            }
        }

        stage('Test') {
           parallel([
               shared: { sh "./gradlew :shared:test jacocoTestReport" },
               client: { sh "./gradlew :client:test jacocoTestReport" },
               server: { sh "./gradlew :server:test jacocoTestReport" },
               swing:  { sh "./gradlew :swing:test  jacocoTestReport" },
           ])
        }

        stage('Findbugs') {
            parallel([
                shared: { sh "./gradlew :shared:findbugsMain" },
                client: { sh "./gradlew :client:findbugsMain" },
                server: { sh "./gradlew :server:findbugsMain" },
                swing:  { sh "./gradlew :swing:findbugsMain " },
            ])
        }

        stage('Results') {
            steps {
                junit '**/test-results/test/TEST-*.xml'
                setBuildStatus("Build complete", "SUCCESS");

                archiveArtifacts artifacts: '*/build/distributions/*.zip'

                zip zipFile: 'coverage.zip', glob: '*/build/reports/jacoco/', archive: true

                sh "rm -rf */build/reports/"
            }
        }

/*
        stage('Docker') {
            steps {
                sh "docker build --tag=postit-server docker/Dockerfile.server ."
                sh "docker tag postit-server nishadmathur.com/postit-server"
                sh "docker image push nishadmathur.com/postit-server"
            }
        }
*/
    }
}
